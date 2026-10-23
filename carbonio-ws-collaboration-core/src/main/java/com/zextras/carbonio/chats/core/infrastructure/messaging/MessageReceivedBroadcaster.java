// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.MessageReceived;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class MessageReceivedBroadcaster {

  private final SubscriptionRepository subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;
  private final ObjectMapper objectMapper;

  @Inject
  public MessageReceivedBroadcaster(
      SubscriptionRepository subscriptionRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      EventDispatcher eventDispatcher,
      ObjectMapper objectMapper) {
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
  }

  /**
   * Broadcasts a MessageReceived event to the room members. Members who cannot see the quoted
   * original (it is at or before their cleared-history instant) receive the same event without the
   * embedded repliedMessage and without replyToId.
   */
  public void broadcast(String roomId, TimelineMessageDto message, @Nullable String tempId)
      throws JsonProcessingException {
    broadcastWithReplyVisibility(
        roomId,
        MessageReceived.create().message(message).tempId(tempId).sentDate(OffsetDateTime.now()),
        message);
  }

  /**
   * Broadcasts an event whose top-level "message" field is the given message, applying the same
   * per-recipient reply visibility as {@link #broadcast}.
   */
  public void broadcastWithReplyVisibility(String roomId, WsEvent event, TimelineMessageDto message)
      throws JsonProcessingException {
    List<String> memberIds = subscriptionRepository.getUserIdsByRoomId(roomId);
    String json = objectMapper.writeValueAsString(event);
    TimelineMessageDto replied = message.getRepliedMessage();
    if (replied == null) {
      eventDispatcher.sendRawToUserExchange(
          memberIds, message.getReplyToId() == null ? json : withoutQuote(json));
      return;
    }
    if (replied.getCreatedAt() == null) {
      eventDispatcher.sendRawToUserExchange(memberIds, withoutQuote(json));
      return;
    }

    Set<String> denied = deniedUsers(roomId, replied.getCreatedAt());
    List<String> withQuote = memberIds.stream().filter(id -> !denied.contains(id)).toList();
    List<String> withoutQuote = memberIds.stream().filter(denied::contains).toList();
    if (!withQuote.isEmpty()) {
      eventDispatcher.sendRawToUserExchange(withQuote, json);
    }
    if (!withoutQuote.isEmpty()) {
      eventDispatcher.sendRawToUserExchange(withoutQuote, withoutQuote(json));
    }
  }

  private String withoutQuote(String json) throws JsonProcessingException {
    JsonNode node = objectMapper.readTree(json);
    ((ObjectNode) node.get("message")).remove(List.of("repliedMessage", "replyToId"));
    return objectMapper.writeValueAsString(node);
  }

  /** Sends a pre-serialized event only to the members who can see an item created at that time. */
  public void broadcastToMembersWhoCanAccess(
      String roomId, OffsetDateTime itemCreatedAt, String json) {
    Set<String> denied = deniedUsers(roomId, itemCreatedAt);
    List<String> allowed =
        subscriptionRepository.getUserIdsByRoomId(roomId).stream()
            .filter(id -> !denied.contains(id))
            .toList();
    if (!allowed.isEmpty()) {
      eventDispatcher.sendRawToUserExchange(allowed, json);
    }
  }

  private Set<String> deniedUsers(String roomId, OffsetDateTime itemCreatedAt) {
    return new HashSet<>(
        roomUserSettingsRepository.getUserIdsClearedAtOrAfter(roomId, itemCreatedAt));
  }
}
