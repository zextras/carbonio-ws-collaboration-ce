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
import java.util.function.Predicate;

@Singleton
public class MessageBrokerVideoserverHealthMonitor {

  private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration CHECK_INTERVAL = Duration.ofSeconds(5);

  private final EventWebSocketSessions sessions;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Object lock = new Object();
  private boolean brokerConnected = true;
  private final Map<String, VideoServerStatus> videoServers = new HashMap<>();
  private boolean available;

  private record VideoServerStatus(Instant seenAt, long sentAt, boolean running, boolean silent) {}

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
    track(emitter, sentAt, true);
  }

  public void notifyVideoServerDown(String emitter, long sentAt) {
    track(emitter, sentAt, false);
  }

  private void track(String emitter, long sentAt, boolean running) {
    synchronized (lock) {
      VideoServerStatus last = videoServers.get(emitter);
      if (last != null && sentAt > 0 && sentAt < last.sentAt()) return;
      if (last == null || last.running() != running || last.silent()) {
        ChatsLogger.info("Video server '" + emitter + "' is " + (running ? "up" : "shut down"));
      }
      long latest = last == null ? sentAt : Math.max(sentAt, last.sentAt());
      videoServers.put(emitter, new VideoServerStatus(clock.instant(), latest, running, false));
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
      VideoServerStatus status = videoServers.get(serverId);
      return available && (status == null || isUp(status));
    }
  }

  public boolean isDown(String serverId) {
    return matches(serverId, status -> !isUp(status));
  }

  public boolean isShutDown(String serverId) {
    return matches(serverId, status -> !status.running());
  }

  // no server id: the meeting's video server is unknown, so every known one must match
  private boolean matches(String serverId, Predicate<VideoServerStatus> state) {
    synchronized (lock) {
      if (serverId == null) {
        return !videoServers.isEmpty() && videoServers.values().stream().allMatch(state);
      }
      VideoServerStatus status = videoServers.get(serverId);
      return status != null && state.test(status);
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
          (emitter, status) -> {
            if (!status.running() || status.silent() || isFresh(status.seenAt())) return status;
            ChatsLogger.warn(
                "Video server '"
                    + emitter
                    + "' sent no heartbeat for "
                    + HEARTBEAT_TIMEOUT.toSeconds()
                    + " seconds");
            return new VideoServerStatus(status.seenAt(), status.sentAt(), true, true);
          });
      update();
    }
  }

  private void update() {
    boolean nowAvailable = brokerConnected && videoServers.values().stream().anyMatch(this::isUp);
    if (available == nowAvailable) return;
    available = nowAvailable;
    send(available ? restored() : disconnected(), sessions::broadcast);
  }

  private boolean isUp(VideoServerStatus status) {
    return status.running() && isFresh(status.seenAt());
  }

  private boolean isFresh(Instant heartbeat) {
    return !clock.instant().isAfter(heartbeat.plus(HEARTBEAT_TIMEOUT));
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
