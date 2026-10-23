// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.model.ChatMessageTimelineItemDto;
import com.zextras.carbonio.chats.model.DeletedInfoDto;
import com.zextras.carbonio.chats.model.EditedInfoDto;
import com.zextras.carbonio.chats.model.ForwardedInfoDto;
import com.zextras.carbonio.chats.model.MessageAttachmentDto;
import com.zextras.carbonio.chats.model.ReactionGroupDto;
import com.zextras.carbonio.chats.model.SystemEventDto;
import com.zextras.carbonio.chats.model.SystemEventTimelineItemDto;
import com.zextras.carbonio.chats.model.TimelineItemDto;
import com.zextras.carbonio.chats.model.TimelineItemTypeDto;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class TimelineItemMapper {

  private final ObjectMapper objectMapper;

  @Inject
  public TimelineItemMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  // Scalar/attachment fields shared by a top-level timeline message and its embedded reply target.
  // Never sets reactions or the nested repliedMessage (those belong to the top level only).
  public TimelineMessageDto baseMessageFields(
      Message msg, Map<String, FileMetadata> attachmentsById) {
    TimelineMessageDto messageDto =
        TimelineMessageDto.create()
            .id(msg.getId())
            .roomId(msg.getRoomId())
            .senderId(msg.getSenderId())
            .text(msg.getText() != null ? msg.getText() : "")
            .createdAt(msg.getCreatedAt());

    if (msg.getReplyToId() != null) {
      messageDto.replyToId(msg.getReplyToId());
    }

    if (Boolean.TRUE.equals(msg.isEdited()) && msg.getUpdatedAt() != null) {
      messageDto.editedInfo(EditedInfoDto.create().editedAt(msg.getUpdatedAt()));
    }

    if (Boolean.TRUE.equals(msg.isDeleted())) {
      messageDto.deletedInfo(
          DeletedInfoDto.create()
              .deletedBy(msg.getSenderId())
              .deletedAt(msg.getDeletedAt() != null ? msg.getDeletedAt() : msg.getUpdatedAt()));
    }

    if (Boolean.TRUE.equals(msg.isForwarded()) && msg.getForwardedSenderId() != null) {
      messageDto.forwardedInfo(
          ForwardedInfoDto.create()
              .originalSenderId(msg.getForwardedSenderId())
              .originalSentAt(
                  msg.getForwardedAt() != null ? msg.getForwardedAt() : msg.getCreatedAt()));
    } else if (Boolean.TRUE.equals(msg.isForwarded())) {
      // Fallback for rows without the new columns (pre-migration data)
      messageDto.forwardedInfo(
          ForwardedInfoDto.create()
              .originalSenderId(msg.getSenderId())
              .originalSentAt(msg.getCreatedAt()));
    }

    // Resolve attachment if present (pre-fetched by the caller in a single batch query)
    if (msg.getAttachmentId() != null) {
      FileMetadata meta = attachmentsById.get(msg.getAttachmentId());
      if (meta != null) {
        messageDto.attachment(
            MessageAttachmentDto.create()
                .id(meta.getId())
                .name(meta.getName())
                .mimeType(meta.getMimeType())
                .size(meta.getOriginalSize())
                .area(meta.getArea()));
      }
    }

    return messageDto;
  }

  public ChatMessageTimelineItemDto toMessageTimelineItem(
      Message msg, Map<String, FileMetadata> attachmentsById, Map<String, Message> repliedById) {
    TimelineMessageDto messageDto = baseMessageFields(msg, attachmentsById);

    // Group reactions by emoji (top-level only; the reply preview never shows them)
    List<MessageReaction> reactions = msg.getReactions();
    if (reactions != null && !reactions.isEmpty()) {
      Map<String, List<String>> reactionGroups = new LinkedHashMap<>();
      for (MessageReaction r : reactions) {
        reactionGroups.computeIfAbsent(r.getReaction(), k -> new ArrayList<>()).add(r.getUserId());
      }
      List<ReactionGroupDto> reactionList =
          reactionGroups.entrySet().stream()
              .map(e -> ReactionGroupDto.create().reaction(e.getKey()).userIds(e.getValue()))
              .collect(Collectors.toList());
      messageDto.reactions(reactionList);
    }

    // Explode the reply target inline, depth-1 (the embedded message carries no nested reply).
    // Absent from the map => original not visible to the reader => the message looks like a normal
    // one, without replyToId either.
    if (msg.getReplyToId() != null) {
      Message original = repliedById.get(msg.getReplyToId());
      if (original != null) {
        messageDto.repliedMessage(baseMessageFields(original, attachmentsById));
      } else {
        messageDto.replyToId(null);
      }
    }

    ChatMessageTimelineItemDto item = ChatMessageTimelineItemDto.create();
    item.itemType(TimelineItemTypeDto.MESSAGE);
    item.createdAt(msg.getCreatedAt());
    item.message(messageDto);
    return item;
  }

  public TimelineItemDto toTimelineItem(
      TimelineItem item,
      Map<String, FileMetadata> attachmentsById,
      Map<String, Message> repliedById) {
    return switch (item) {
      case TimelineItem.MessageItem messageItem ->
          toMessageTimelineItem(messageItem.message(), attachmentsById, repliedById);
      case TimelineItem.SystemEventItem eventItem -> toSystemEventTimelineItem(eventItem.event());
    };
  }

  private SystemEventTimelineItemDto toSystemEventTimelineItem(SystemEvent event) {
    SystemEventDto eventDto =
        SystemEventDto.create()
            .id(event.getId())
            .roomId(event.getRoomId())
            .type(event.getType())
            .actorId(event.getActorId())
            .createdAt(event.getCreatedAt());

    // Parse the JSON content string back into a map for the response
    if (event.getContent() != null && !event.getContent().isBlank()) {
      try {
        Map<String, Object> contentMap =
            objectMapper.readValue(event.getContent(), new TypeReference<Map<String, Object>>() {});
        eventDto.content(contentMap);
      } catch (Exception e) {
        eventDto.content(Map.of());
      }
    } else {
      eventDto.content(Map.of());
    }

    SystemEventTimelineItemDto item = SystemEventTimelineItemDto.create();
    item.itemType(TimelineItemTypeDto.SYSTEM_EVENT);
    item.createdAt(event.getCreatedAt());
    item.systemEvent(eventDto);
    return item;
  }
}
