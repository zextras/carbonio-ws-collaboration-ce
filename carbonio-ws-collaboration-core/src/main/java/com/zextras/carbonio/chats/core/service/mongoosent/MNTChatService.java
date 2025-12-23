// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessage;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageReaction;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageRead;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoom;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoomMember;
import com.zextras.carbonio.chats.core.data.model.message.InboxItemDto;
import com.zextras.carbonio.chats.core.data.model.message.MessageDto;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTMessageReadRepository;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTMessageRepository;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTRoomRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class MNTChatService {

  private static final int DEFAULT_LIMIT = 100;

  private final MNTRoomRepository roomRepository;
  private final MNTMessageRepository messageRepository;
  private final MNTMessageReadRepository messageReadRepository;

  @Inject
  public MNTChatService(
      MNTRoomRepository roomRepository,
      MNTMessageRepository messageRepository,
      MNTMessageReadRepository messageReadRepository) {
    this.roomRepository = roomRepository;
    this.messageRepository = messageRepository;
    this.messageReadRepository = messageReadRepository;
  }

  // ============================================================================
  // Room Operations
  // ============================================================================

  public MNTRoom createRoom(
      String creatorId, MNTRoom.RoomType type, String name, String description, List<String> memberIds) {

    // For 1-to-1 chats, check if room already exists
    if (type == MNTRoom.RoomType.ONE_TO_ONE && memberIds.size() == 1) {
      String otherId = memberIds.get(0);
      Optional<MNTRoom> existingRoom = roomRepository.findOneToOneRoom(creatorId, otherId);
      if (existingRoom.isPresent()) {
        return existingRoom.get();
      }
    }

    MNTRoom room =
        MNTRoom.create()
            .id(UUID.randomUUID().toString())
            .type(type)
            .name(name)
            .description(description)
            .createdBy(creatorId);

    roomRepository.insert(room);

    // Add creator as owner
    MNTRoomMember creatorMember = MNTRoomMember.create(room, creatorId).owner(true);
    roomRepository.addMember(creatorMember);

    // Add other members
    for (String memberId : memberIds) {
      if (!memberId.equals(creatorId)) {
        MNTRoomMember member = MNTRoomMember.create(room, memberId).owner(false);
        roomRepository.addMember(member);
      }
    }

    return roomRepository.getById(room.getId()).orElse(room);
  }

  public Optional<MNTRoom> getRoom(String roomId) {
    return roomRepository.getById(roomId);
  }

  public List<String> getRoomMemberIds(String roomId) {
    return roomRepository.getMemberIds(roomId);
  }

  public void validateUserInRoom(String roomId, String userId) {
    if (!roomRepository.isMember(roomId, userId)) {
      throw new ForbiddenException("User is not a member of this room");
    }
  }

  // ============================================================================
  // Inbox
  // ============================================================================

  public List<InboxItemDto> getInbox(String userId) {
    List<MNTRoom> rooms = roomRepository.getByUserId(userId);
    List<InboxItemDto> inbox = new ArrayList<>();

    for (MNTRoom room : rooms) {
      Optional<MNTMessage> lastMessage = messageRepository.getLastByRoomId(room.getId());
      Optional<MNTMessageRead> readMarker =
          messageReadRepository.getByUserIdAndRoomId(userId, room.getId());

      long unreadCount =
          messageRepository.countUnreadMessages(
              room.getId(), userId, readMarker.map(MNTMessageRead::getMessageId).orElse(null));

      // Check if muted
      boolean muted =
          room.getMembers().stream()
              .filter(m -> m.getUserId().equals(userId))
              .findFirst()
              .map(MNTRoomMember::isMuted)
              .orElse(false);

      List<String> memberIds =
          room.getMembers().stream().map(MNTRoomMember::getUserId).toList();

      InboxItemDto item =
          InboxItemDto.create()
              .roomId(room.getId())
              .roomName(room.getName())
              .roomType(room.getType() != null ? room.getType().toString() : null)
              .lastMessage(lastMessage.map(this::toDto).orElse(null))
              .unreadCount(unreadCount)
              .muted(muted)
              .members(memberIds);

      inbox.add(item);
    }

    return inbox;
  }

  // ============================================================================
  // Message Operations
  // ============================================================================

  public MessageDto sendMessage(
      String roomId, String senderId, String text, String replyToId, String forwardedFromId, String forwardedBy) {
    validateUserInRoom(roomId, senderId);

    MNTMessage message =
        MNTMessage.create()
            .id(UUID.randomUUID().toString())
            .roomId(roomId)
            .senderId(senderId)
            .text(text)
            .replyToId(replyToId)
            .forwardedFromId(forwardedFromId)
            .forwardedBy(forwardedBy);

    messageRepository.insert(message);
    return toDto(message);
  }

  public MessageDto editMessage(String messageId, String userId, String newText) {
    MNTMessage message = getMessageEntity(messageId);

    if (!message.getSenderId().equals(userId)) {
      throw new ForbiddenException("Only the sender can edit a message");
    }

    if (message.getForwardedFromId() != null) {
      throw new ForbiddenException("Forwarded messages cannot be edited");
    }

    message.text(newText).edited(true);
    messageRepository.update(message);
    return toDto(message);
  }

  public void deleteMessage(String messageId, String userId) {
    MNTMessage message = getMessageEntity(messageId);

    if (!message.getSenderId().equals(userId)) {
      throw new ForbiddenException("Only the sender can delete a message");
    }

    messageRepository.markAsDeleted(messageId);
  }

  public MessageDto getMessageById(String messageId) {
    return toDto(getMessageEntity(messageId));
  }

  public List<MessageDto> getHistory(String roomId, String userId, int limit, String beforeMessageId) {
    validateUserInRoom(roomId, userId);
    int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
    List<MNTMessage> messages = messageRepository.getByRoomId(roomId, effectiveLimit, beforeMessageId);
    return messages.stream().map(this::toDto).collect(Collectors.toList());
  }

  public List<MessageDto> getHistoryAfter(String roomId, String userId, int limit, String afterMessageId) {
    validateUserInRoom(roomId, userId);
    int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
    List<MNTMessage> messages = messageRepository.getByRoomIdAfter(roomId, effectiveLimit, afterMessageId);
    // Already in ascending order (oldest first), which is what frontend expects
    return messages.stream().map(this::toDto).collect(Collectors.toList());
  }

  public List<MessageDto> searchMessages(String roomId, String userId, String searchText, int limit) {
    validateUserInRoom(roomId, userId);
    int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
    List<MNTMessage> messages = messageRepository.searchByText(roomId, searchText, effectiveLimit);
    return messages.stream().map(this::toDto).collect(Collectors.toList());
  }

  public MessageDto forwardMessage(String messageId, String userId, String targetRoomId) {
    MNTMessage original = getMessageEntity(messageId);
    validateUserInRoom(targetRoomId, userId);

    return sendMessage(
        targetRoomId,
        userId,
        original.getText(),
        null,
        original.getId(),
        userId);
  }

  public List<MessageDto> getMessagesAround(String roomId, String userId, String messageId, int limit) {
    validateUserInRoom(roomId, userId);
    int half = limit / 2;
    List<MNTMessage> messages = messageRepository.getMessagesAround(roomId, messageId, half, half);
    return messages.stream().map(this::toDto).collect(Collectors.toList());
  }

  // ============================================================================
  // Reactions
  // ============================================================================

  public void addReaction(String messageId, String userId, String reaction) {
    MNTMessage message = getMessageEntity(messageId);
    validateUserInRoom(message.getRoomId(), userId);

    // Check if reaction already exists
    if (messageRepository.hasReaction(messageId, userId, reaction)) {
      throw new BadRequestException("Reaction already exists");
    }

    MNTMessageReaction reactionEntity = MNTMessageReaction.create(message, userId, reaction);
    messageRepository.addReaction(reactionEntity);
  }

  public void removeReaction(String messageId, String userId, String reaction) {
    MNTMessage message = getMessageEntity(messageId);
    validateUserInRoom(message.getRoomId(), userId);
    messageRepository.removeReaction(messageId, userId, reaction);
  }

  // ============================================================================
  // Read Markers
  // ============================================================================

  public void markAsRead(String userId, String roomId, String messageId) {
    validateUserInRoom(roomId, userId);
    messageReadRepository.upsert(userId, roomId, messageId);
  }

  public Map<String, String> getReadStatus(String roomId, String userId) {
    validateUserInRoom(roomId, userId);
    List<MNTMessageRead> markers = messageReadRepository.getByRoomId(roomId);
    Map<String, String> status = new HashMap<>();
    for (MNTMessageRead marker : markers) {
      status.put(marker.getUserId(), marker.getMessageId());
    }
    return status;
  }

  // ============================================================================
  // Private Helpers
  // ============================================================================

  private MNTMessage getMessageEntity(String messageId) {
    return messageRepository
        .getById(messageId)
        .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));
  }

  private MessageDto toDto(MNTMessage message) {
    MessageDto dto =
        MessageDto.create()
            .id(message.getId())
            .roomId(message.getRoomId())
            .senderId(message.getSenderId())
            .text(message.getText())
            .replyToId(message.getReplyToId())
            .forwardedFromId(message.getForwardedFromId())
            .edited(message.isEdited())
            .deleted(message.isDeleted())
            .createdAt(message.getCreatedAt());

    // Load reply-to message (shallow)
    if (message.getReplyToId() != null) {
      messageRepository
          .getById(message.getReplyToId())
          .ifPresent(
              reply ->
                  dto.replyTo(
                      MessageDto.create()
                          .id(reply.getId())
                          .senderId(reply.getSenderId())
                          .text(reply.getText())
                          .createdAt(reply.getCreatedAt())));
    }

    // Load forwarded-from message (shallow)
    if (message.getForwardedFromId() != null) {
      messageRepository
          .getById(message.getForwardedFromId())
          .ifPresent(
              forwarded ->
                  dto.forwardedFrom(
                      MessageDto.create()
                          .id(forwarded.getId())
                          .senderId(forwarded.getSenderId())
                          .text(forwarded.getText())
                          .createdAt(forwarded.getCreatedAt())));
    }

    // Load reactions
    List<MNTMessageReaction> reactions = messageRepository.getReactionsByMessageId(message.getId());
    if (!reactions.isEmpty()) {
      Map<String, List<String>> reactionMap = new HashMap<>();
      for (MNTMessageReaction r : reactions) {
        reactionMap.computeIfAbsent(r.getReaction(), k -> new ArrayList<>()).add(r.getUserId());
      }
      dto.reactions(reactionMap);
    }

    return dto;
  }
}
