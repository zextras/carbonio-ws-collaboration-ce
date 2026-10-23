// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.PresenceChanged;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Broadcasts presence-change events to the subscribers of a user.
 *
 * <p>This class encapsulates the logic of finding who should be notified when a user's
 * online/offline status changes and dispatching the event to each subscriber via the {@link
 * EventDispatcher}.
 */
@Singleton
public class PresenceBroadcaster {

  private final ChatMessageService chatMessageService;
  private final EventDispatcher eventDispatcher;
  private final ObjectMapper objectMapper;

  @Inject
  public PresenceBroadcaster(
      ChatMessageService chatMessageService,
      EventDispatcher eventDispatcher,
      ObjectMapper objectMapper) {
    this.chatMessageService = chatMessageService;
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
  }

  /**
   * Sends a {@link PresenceChangedEvent} for {@code userId} to all relevant subscribers.
   *
   * <p>The user themselves is excluded from the recipient list.
   *
   * @param userId the user whose presence changed
   * @param online {@code true} if the user just came online, {@code false} if they went offline
   * @param lastActivity last time the user went offline, {@code null} if never
   */
  public void broadcastPresence(String userId, boolean online, OffsetDateTime lastActivity) {
    try {
      WsEvent event =
          PresenceChanged.create()
              .userId(userId)
              .online(online)
              .lastActivity(lastActivity)
              .sentDate(OffsetDateTime.now());
      String json = objectMapper.writeValueAsString(event);

      // Notify all users who should receive presence updates for this user
      List<String> subscribers = chatMessageService.getPresenceSubscribers(userId);
      for (String subscriberId : subscribers) {
        if (!subscriberId.equals(userId)) {
          eventDispatcher.sendRawToUserExchange(subscriberId, json);
        }
      }
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Error broadcasting presence change for user '%s': %s", userId, e.getMessage()));
    }
  }
}
