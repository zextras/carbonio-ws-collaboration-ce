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
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class MessageBrokerHealthMonitor {

  private final EventWebSocketSessions sessions;
  private final ObjectMapper objectMapper;

  private final AtomicBoolean messageBrokerDown = new AtomicBoolean(false);
  private final AtomicBoolean videoServerReady = new AtomicBoolean(false);
  private final AtomicBoolean pendingRestoredNotification = new AtomicBoolean(false);

  @Inject
  public MessageBrokerHealthMonitor(EventWebSocketSessions sessions, ObjectMapper objectMapper) {
    this.sessions = sessions;
    this.objectMapper = objectMapper;
  }

  public void notifyBrokerDown() {
    if (!messageBrokerDown.compareAndSet(false, true)) return;
    videoServerReady.set(false);
    pendingRestoredNotification.set(false);
    broadcast(buildDisconnected());
  }

  public void notifyBrokerRecovered() {
    if (!messageBrokerDown.compareAndSet(true, false)) return;
    if (videoServerReady.get()) {
      broadcast(buildRestored());
    } else {
      pendingRestoredNotification.set(true);
    }
  }

  public void notifyVideoServerReady() {
    if (!videoServerReady.compareAndSet(false, true)) return;
    if (pendingRestoredNotification.compareAndSet(true, false)) {
      broadcast(buildRestored());
    }
  }

  public boolean isReadyForUser(String userId) {
    return !messageBrokerDown.get()
        && videoServerReady.get()
        && sessions.hasActiveSessionForUser(userId);
  }

  private DomainEvent buildDisconnected() {
    return MessageBrokerDisconnected.create()
        .type(EventType.MESSAGE_BROKER_DISCONNECTED)
        .sentDate(OffsetDateTime.now());
  }

  private DomainEvent buildRestored() {
    return MessageBrokerRestored.create()
        .type(EventType.MESSAGE_BROKER_RESTORED)
        .sentDate(OffsetDateTime.now());
  }

  private void broadcast(DomainEvent event) {
    try {
      sessions.broadcast(objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.error("Failed to serialize broker health event", e);
    }
  }
}
