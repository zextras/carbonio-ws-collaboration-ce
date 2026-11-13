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
  private Instant lastHeartbeat = Instant.MIN;
  private boolean available;

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

  public void notifyVideoServerReady() {
    synchronized (lock) {
      lastHeartbeat = clock.instant();
      update();
    }
  }

  public void notifyBrokerDown() {
    synchronized (lock) {
      brokerConnected = false;
      lastHeartbeat = Instant.MIN;
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

  public void notifyIfUnavailable(Session session) {
    synchronized (lock) {
      if (!available) send(disconnected(), session.getAsyncRemote()::sendText);
    }
  }

  private void expireStaleHeartbeat() {
    synchronized (lock) {
      update();
    }
  }

  private void update() {
    boolean nowAvailable =
        brokerConnected && !clock.instant().isAfter(lastHeartbeat.plus(HEARTBEAT_TIMEOUT));
    if (available == nowAvailable) return;
    available = nowAvailable;
    send(available ? restored() : disconnected(), sessions::broadcast);
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
