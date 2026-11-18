// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

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
import java.util.function.Function;

@Singleton
public class MessageReceivedBroadcaster {

  private final SubscriptionRepository subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;

  @Inject
  public MessageReceivedBroadcaster(
      SubscriptionRepository subscriptionRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      EventDispatcher eventDispatcher) {
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
  }

  /**
   * Broadcasts a MessageReceived event to the room members. Members who cannot see the quoted
   * original (it is at or before their cleared-history instant) receive the same event without the
   * embedded repliedMessage and without replyToId.
   */
  public void broadcast(String roomId, TimelineMessageDto message, @Nullable String tempId) {
    broadcastWithReplyVisibility(
        roomId, message, visible -> MessageReceived.create().message(visible).tempId(tempId));
  }

  /**
   * Broadcasts an event built around the given message, applying the same per-recipient reply
   * visibility as {@link #broadcast}. The factory is called with the message as is for the
   * recipients who can see the quote, and with the message stripped of the quote for the others.
   */
  public void broadcastWithReplyVisibility(
      String roomId, TimelineMessageDto message, Function<TimelineMessageDto, WsEvent> event) {
    List<String> memberIds = subscriptionRepository.getUserIdsByRoomId(roomId);
    TimelineMessageDto replied = message.getRepliedMessage();
    if (replied == null) {
      eventDispatcher.sendWsEvent(
          memberIds, event.apply(message.getReplyToId() == null ? message : withoutQuote(message)));
      return;
    }
    if (replied.getCreatedAt() == null) {
      eventDispatcher.sendWsEvent(memberIds, event.apply(withoutQuote(message)));
      return;
    }

    Set<String> denied = deniedUsers(roomId, replied.getCreatedAt());
    List<String> withQuote = memberIds.stream().filter(id -> !denied.contains(id)).toList();
    List<String> withoutQuote = memberIds.stream().filter(denied::contains).toList();
    if (!withQuote.isEmpty()) {
      eventDispatcher.sendWsEvent(withQuote, event.apply(message));
    }
    if (!withoutQuote.isEmpty()) {
      eventDispatcher.sendWsEvent(withoutQuote, event.apply(withoutQuote(message)));
    }
  }

  private static TimelineMessageDto withoutQuote(TimelineMessageDto message) {
    return TimelineMessageDto.create()
        .id(message.getId())
        .roomId(message.getRoomId())
        .senderId(message.getSenderId())
        .text(message.getText())
        .createdAt(message.getCreatedAt())
        .editedInfo(message.getEditedInfo())
        .deletedInfo(message.getDeletedInfo())
        .forwardedInfo(message.getForwardedInfo())
        .attachment(message.getAttachment())
        .reactions(message.getReactions());
  }

  /** Sends an event only to the members who can see an item created at that time. */
  public void broadcastToMembersWhoCanAccess(
      String roomId, OffsetDateTime itemCreatedAt, WsEvent event) {
    List<String> allowed = subscriptionRepository.getUserIdsWhoCanAccess(roomId, itemCreatedAt);
    if (!allowed.isEmpty()) {
      eventDispatcher.sendWsEvent(allowed, event);
    }
  }

  private Set<String> deniedUsers(String roomId, OffsetDateTime itemCreatedAt) {
    return new HashSet<>(
        roomUserSettingsRepository.getUserIdsClearedAtOrAfter(roomId, itemCreatedAt));
  }
}
