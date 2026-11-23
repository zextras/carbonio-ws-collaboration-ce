// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

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
import jakarta.websocket.Session;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class MessageBrokerVideoserverHealthMonitor {

  private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration CHECK_INTERVAL = Duration.ofSeconds(5);

  private final EventWebSocketSessions sessions;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Object lock = new Object();
  private boolean brokerConnected = true;
  private final Map<String, VideoServer> videoServers = new HashMap<>();
  private boolean available;

  private enum State {
    UP,
    SILENT,
    SHUT_DOWN
  }

  private record VideoServer(State state, long sentAt, Instant seenAt) {}

  @Inject
  public MessageBrokerVideoserverHealthMonitor(
      EventWebSocketSessions sessions, ObjectMapper objectMapper, Clock clock) {
    this(
        sessions,
        objectMapper,
        clock,
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("Video server heartbeat check").daemon().factory()));
  }

  MessageBrokerVideoserverHealthMonitor(
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

  public void notifyVideoServerReady(String emitter) {
    notifyVideoServerReady(emitter, 0);
  }

  public void notifyVideoServerReady(String emitter, long sentAt) {
    track(emitter, sentAt, State.UP);
  }

  public void notifyVideoServerDown(String emitter, long sentAt) {
    track(emitter, sentAt, State.SHUT_DOWN);
  }

  private void track(String emitter, long sentAt, State state) {
    synchronized (lock) {
      VideoServer last = videoServers.get(emitter);
      if (last != null && sentAt > 0 && sentAt < last.sentAt()) return;
      if (last == null ? state == State.SHUT_DOWN : last.state() != state) log(emitter, state);
      long latest = last == null ? sentAt : Math.max(sentAt, last.sentAt());
      videoServers.put(emitter, new VideoServer(state, latest, clock.instant()));
      update();
    }
  }

  public void notifyBrokerDown() {
    synchronized (lock) {
      brokerConnected = false;
      videoServers.clear();
      update();
    }
  }

  public void notifyBrokerRecovered() {
    synchronized (lock) {
      brokerConnected = true;
      update();
    }
  }

  public void checkStatus() {
    synchronized (lock) {
      if (!available) {
        throw new VideoServerException("Video server events are not reaching this instance");
      }
    }
  }

  // a server that never named itself in a heartbeat is covered by the others
  public boolean isAvailable(String serverId) {
    synchronized (lock) {
      VideoServer server = videoServers.get(serverId);
      return available && (server == null || server.state() == State.UP);
    }
  }

  // down: shut down, or no heartbeat for 60 s, so it may still be running
  public boolean isDown() {
    synchronized (lock) {
      return !videoServers.isEmpty()
          && videoServers.values().stream().noneMatch(server -> server.state() == State.UP);
    }
  }

  public boolean isDown(String emitter) {
    synchronized (lock) {
      VideoServer server = videoServers.get(emitter);
      return server != null && server.state() != State.UP;
    }
  }

  // shut down: Janus reported it, so its rooms and sessions are gone
  public boolean isShutDown() {
    synchronized (lock) {
      return !videoServers.isEmpty()
          && videoServers.values().stream().allMatch(server -> server.state() == State.SHUT_DOWN);
    }
  }

  public boolean isShutDown(String emitter) {
    synchronized (lock) {
      VideoServer server = videoServers.get(emitter);
      return server != null && server.state() == State.SHUT_DOWN;
    }
  }

  public void notifyIfUnavailable(Session session) {
    synchronized (lock) {
      if (!available) send(disconnected(), session.getAsyncRemote()::sendText);
    }
  }

  private void expireStaleHeartbeat() {
    synchronized (lock) {
      videoServers.replaceAll(
          (emitter, server) -> {
            if (server.state() != State.UP || isFresh(server.seenAt())) return server;
            log(emitter, State.SILENT);
            return new VideoServer(State.SILENT, server.sentAt(), server.seenAt());
          });
      update();
    }
  }

  private void update() {
    boolean nowAvailable =
        brokerConnected
            && videoServers.values().stream().anyMatch(server -> server.state() == State.UP);
    if (available == nowAvailable) return;
    available = nowAvailable;
    send(available ? restored() : disconnected(), sessions::broadcast);
  }

  private boolean isFresh(Instant heartbeat) {
    return !clock.instant().isAfter(heartbeat.plus(HEARTBEAT_TIMEOUT));
  }

  private static void log(String emitter, State state) {
    switch (state) {
      case UP -> ChatsLogger.info("Video server '" + emitter + "' is up");
      case SHUT_DOWN -> ChatsLogger.info("Video server '" + emitter + "' is shut down");
      case SILENT ->
          ChatsLogger.warn(
              "Video server '"
                  + emitter
                  + "' sent no heartbeat for "
                  + HEARTBEAT_TIMEOUT.toSeconds()
                  + " seconds");
    }
  }

  private DomainEvent disconnected() {
    return MessageBrokerDisconnected.create()
        .type(EventType.MESSAGE_BROKER_DISCONNECTED)
        .sentDate(OffsetDateTime.now());
  }

  private DomainEvent restored() {
    return MessageBrokerRestored.create()
        .type(EventType.MESSAGE_BROKER_RESTORED)
        .sentDate(OffsetDateTime.now());
  }

  private void send(DomainEvent event, Consumer<String> recipients) {
    try {
      recipients.accept(objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.error("Failed to serialize broker health event", e);
    }
  }
}
