// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MessageBrokerDisconnected;
import com.zextras.carbonio.async.model.MessageBrokerRestored;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import jakarta.websocket.Session;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class VideoServerMonitor {

  private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration CHECK_INTERVAL = Duration.ofSeconds(5);
  // relays swap events by far less; an older event arriving later means the Janus clock went back
  private static final Duration REORDER_WINDOW = Duration.ofSeconds(10);

  private final EventWebSocketSessions sessions;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Map<String, VideoServer> videoServers = new HashMap<>();
  private boolean brokerConnected = true;
  private boolean available;

  private enum State {
    UP,
    SILENT,
    SHUT_DOWN
  }

  private record VideoServer(State state, long sentAt, Instant seenAt) {
    VideoServer silenced() {
      return state == State.UP ? new VideoServer(State.SILENT, sentAt, seenAt) : this;
    }
  }

  @Inject
  public VideoServerMonitor(
      EventWebSocketSessions sessions, ObjectMapper objectMapper, Clock clock) {
    this(
        sessions,
        objectMapper,
        clock,
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("Video server heartbeat check").daemon().factory()));
  }

  VideoServerMonitor(
      EventWebSocketSessions sessions,
      ObjectMapper objectMapper,
      Clock clock,
      ScheduledExecutorService scheduler) {
    this.sessions = sessions;
    this.objectMapper = objectMapper;
    this.clock = clock;
    scheduler.scheduleWithFixedDelay(
        this::expireStaleHeartbeat,
        CHECK_INTERVAL.toMillis(),
        CHECK_INTERVAL.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  public void onHeartbeat(String emitter, long sentAt) {
    track(emitter, sentAt, State.UP);
  }

  public void onShutdown(String emitter, long sentAt) {
    track(emitter, sentAt, State.SHUT_DOWN);
  }

  private synchronized void track(String emitter, long sentAt, State state) {
    // deliveries still queued when the connection dropped run after onBrokerLost
    if (state == State.UP && !brokerConnected) return;
    VideoServer last = videoServers.get(emitter);
    if (last != null && isRelayedOutOfOrder(last, sentAt)) return;
    if (last == null ? state == State.SHUT_DOWN : last.state() != state) log(emitter, state);
    long latest = sentAt == 0 && last != null ? last.sentAt() : sentAt;
    videoServers.put(emitter, new VideoServer(state, latest, clock.instant()));
    update();
  }

  private boolean isRelayedOutOfOrder(VideoServer last, long sentAt) {
    return sentAt > 0
        && sentAt < last.sentAt()
        && clock.instant().isBefore(last.seenAt().plus(REORDER_WINDOW));
  }

  // heartbeats arrive through the broker, so the first one after it is back marks the server up
  public synchronized void onBrokerLost() {
    brokerConnected = false;
    videoServers.replaceAll((emitter, server) -> server.silenced());
    update();
  }

  public synchronized void onBrokerRecovered() {
    brokerConnected = true;
  }

  public synchronized void checkAvailability() {
    if (!available) {
      throw new VideoServerException("Video server events are not reaching this instance");
    }
  }

  // a server that never named itself in a heartbeat is covered by the others
  public synchronized boolean isAvailable(String serverId) {
    VideoServer server = videoServers.get(serverId);
    return available && (server == null || server.state() == State.UP);
  }

  // down: shut down, or no heartbeat for 60 s, so it may still be running
  public synchronized boolean isDown() {
    return !videoServers.isEmpty()
        && videoServers.values().stream().noneMatch(server -> server.state() == State.UP);
  }

  public synchronized boolean isDown(String emitter) {
    VideoServer server = videoServers.get(emitter);
    return server != null && server.state() != State.UP;
  }

  // shut down: Janus reported it, so its rooms and sessions are gone
  public synchronized boolean isShutDown() {
    return !videoServers.isEmpty()
        && videoServers.values().stream().allMatch(server -> server.state() == State.SHUT_DOWN);
  }

  public synchronized boolean isShutDown(String emitter) {
    VideoServer server = videoServers.get(emitter);
    return server != null && server.state() == State.SHUT_DOWN;
  }

  public synchronized void notifyIfUnavailable(Session session) {
    if (!available) send(availabilityEvent(), session.getAsyncRemote()::sendText);
  }

  private synchronized void expireStaleHeartbeat() {
    Instant staleBefore = clock.instant().minus(HEARTBEAT_TIMEOUT);
    videoServers.replaceAll(
        (emitter, server) -> {
          if (server.state() != State.UP || !server.seenAt().isBefore(staleBefore)) return server;
          log(emitter, State.SILENT);
          return server.silenced();
        });
    update();
  }

  private void update() {
    boolean nowAvailable =
        videoServers.values().stream().anyMatch(server -> server.state() == State.UP);
    if (available == nowAvailable) return;
    available = nowAvailable;
    send(availabilityEvent(), sessions::broadcast);
  }

  private static void log(String emitter, State state) {
    String videoServer = "Video server '" + emitter + "' ";
    switch (state) {
      case UP -> ChatsLogger.info(videoServer + "is up");
      case SHUT_DOWN -> ChatsLogger.info(videoServer + "is shut down");
      case SILENT ->
          ChatsLogger.warn(
              videoServer + "sent no heartbeat for " + HEARTBEAT_TIMEOUT.toSeconds() + " seconds");
    }
  }

  private DomainEvent availabilityEvent() {
    OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    return available
        ? MessageBrokerRestored.create().type(EventType.MESSAGE_BROKER_RESTORED).sentDate(now)
        : MessageBrokerDisconnected.create()
            .type(EventType.MESSAGE_BROKER_DISCONNECTED)
            .sentDate(now);
  }

  private void send(DomainEvent event, Consumer<String> recipients) {
    try {
      recipients.accept(objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.error("Failed to serialize broker health event", e);
    }
  }
}
