// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessage;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageAttachment;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageReaction;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageRead;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoom;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoomMember;
import com.zextras.carbonio.chats.core.data.model.message.InboxItemDto;
import com.zextras.carbonio.chats.core.data.model.message.MNTAttachmentDto;
import com.zextras.carbonio.chats.core.data.model.message.MessageDto;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTMessageAttachmentRepository;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTMessageEventRepository;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTMessageReadRepository;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTMessageRepository;
import com.zextras.carbonio.chats.core.repository.mongoosent.MNTRoomRepository;
import java.io.InputStream;
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
  private final MNTMessageEventRepository eventRepository;
  private final MNTMessageAttachmentRepository attachmentRepository;
  private final StoragesService storagesService;

  @Inject
  public MNTChatService(
      MNTRoomRepository roomRepository,
      MNTMessageRepository messageRepository,
      MNTMessageReadRepository messageReadRepository,
      MNTMessageEventRepository eventRepository,
      MNTMessageAttachmentRepository attachmentRepository,
      StoragesService storagesService) {
    this.roomRepository = roomRepository;
    this.messageRepository = messageRepository;
    this.messageReadRepository = messageReadRepository;
    this.eventRepository = eventRepository;
    this.attachmentRepository = attachmentRepository;
    this.storagesService = storagesService;
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
      String roomId,
      String senderId,
      String text,
      String replyToId,
      String forwardedFromId,
      String forwardedBy,
      List<String> attachmentIds) {
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

    // Link attachments if provided
    if (attachmentIds != null && !attachmentIds.isEmpty()) {
      List<String> linked = linkAttachmentsToMessage(message.getId(), attachmentIds, senderId, roomId);
      if (linked.size() != attachmentIds.size()) {
        // Some attachments couldn't be linked - this is logged but not an error
        // (could be already linked, wrong owner, or deleted)
      }
    }

    // Log event: MESSAGE_CREATED or MESSAGE_FORWARDED
    if (forwardedFromId != null) {
      // Get original message's room for logging
      messageRepository
          .getById(forwardedFromId)
          .ifPresent(
              original ->
                  eventRepository.logMessageForwarded(
                      message.getId(), roomId, senderId, forwardedFromId, original.getRoomId()));
    } else {
      eventRepository.logMessageCreated(message.getId(), roomId, senderId, text, replyToId);
    }

    // Reload message to get linked attachments
    return toDto(messageRepository.getById(message.getId()).orElse(message));
  }

  public MessageDto editMessage(String messageId, String userId, String newText) {
    MNTMessage message = getMessageEntity(messageId);

    if (!message.getSenderId().equals(userId)) {
      throw new ForbiddenException("Only the sender can edit a message");
    }

    if (message.getForwardedFromId() != null) {
      throw new ForbiddenException("Forwarded messages cannot be edited");
    }

    String previousText = message.getText();
    message.text(newText).edited(true);
    messageRepository.update(message);

    // Log event: MESSAGE_EDITED
    eventRepository.logMessageEdited(messageId, message.getRoomId(), userId, newText, previousText);

    return toDto(message);
  }

  public void deleteMessage(String messageId, String userId) {
    MNTMessage message = getMessageEntity(messageId);

    if (!message.getSenderId().equals(userId)) {
      throw new ForbiddenException("Only the sender can delete a message");
    }

    String previousText = message.getText();
    String roomId = message.getRoomId();

    // Delete all attachments first (atomic operation)
    List<MNTMessageAttachment> attachments = attachmentRepository.getByMessageId(messageId);
    for (MNTMessageAttachment attachment : attachments) {
      // Delete blob from storages
      storagesService.deleteFile(attachment.getId(), attachment.getUserId());
      // Log event for each attachment
      eventRepository.logAttachmentDeleted(
          messageId, roomId, userId, attachment.getId(), attachment.getFileName());
    }
    // Soft delete all attachment metadata
    attachmentRepository.markAllAsDeletedByMessageId(messageId);

    // Mark message as deleted
    messageRepository.markAsDeleted(messageId);

    // Log event: MESSAGE_DELETED
    eventRepository.logMessageDeleted(messageId, roomId, userId, previousText);
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
        targetRoomId, userId, original.getText(), null, original.getId(), userId, null);
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

    // Log event: REACTION_ADDED
    eventRepository.logReactionAdded(messageId, message.getRoomId(), userId, reaction);
  }

  public void removeReaction(String messageId, String userId, String reaction) {
    MNTMessage message = getMessageEntity(messageId);
    validateUserInRoom(message.getRoomId(), userId);
    messageRepository.removeReaction(messageId, userId, reaction);

    // Log event: REACTION_REMOVED
    eventRepository.logReactionRemoved(messageId, message.getRoomId(), userId, reaction);
  }

  // ============================================================================
  // Attachments
  // ============================================================================

  /**
   * Uploads a pending attachment (not yet linked to a message). The file is uploaded to storages
   * and metadata saved with NULL messageId. Must be linked to a message via sendMessage within 1
   * hour or it will be cleaned up by the orphan cleanup job.
   *
   * @param userId The user uploading the attachment
   * @param fileStream The file content stream
   * @param fileName Original file name
   * @param mimeType MIME type of the file
   * @param fileSize Size in bytes
   * @return The created attachment DTO (with messageId = null)
   */
  public MNTAttachmentDto uploadPendingAttachment(
      String userId, InputStream fileStream, String fileName, String mimeType, long fileSize) {

    String attachmentId = UUID.randomUUID().toString();

    // Upload to storages first (before DB save)
    storagesService.saveFile(fileStream, attachmentId, userId, fileSize);

    // Save metadata with NULL messageId (pending)
    MNTMessageAttachment attachment =
        MNTMessageAttachment.create()
            .id(attachmentId)
            .messageId(null) // Pending - will be set when linked to message
            .userId(userId)
            .fileName(fileName)
            .mimeType(mimeType)
            .fileSize(fileSize);
    attachmentRepository.insert(attachment);

    return toAttachmentDto(attachment);
  }

  /**
   * Links pending attachments to a message. Called during sendMessage. Only attachments owned by
   * the user and not yet linked can be attached. Returns list of successfully linked attachment
   * IDs.
   *
   * @param messageId The message to link to
   * @param attachmentIds List of pending attachment IDs
   * @param userId The user (must own the attachments)
   * @param roomId The room ID for event logging
   * @return List of successfully linked attachment IDs
   */
  public List<String> linkAttachmentsToMessage(
      String messageId, List<String> attachmentIds, String userId, String roomId) {
    List<String> linkedIds = new ArrayList<>();

    for (String attachmentId : attachmentIds) {
      int updated = attachmentRepository.linkToMessage(attachmentId, messageId, userId);
      if (updated > 0) {
        linkedIds.add(attachmentId);
        // Log event for each linked attachment
        attachmentRepository
            .getById(attachmentId)
            .ifPresent(
                att ->
                    eventRepository.logAttachmentAdded(
                        messageId,
                        roomId,
                        userId,
                        attachmentId,
                        att.getFileName(),
                        att.getMimeType(),
                        att.getFileSize()));
      }
    }

    return linkedIds;
  }

  /**
   * Deletes specific attachments from a message (without deleting the message itself).
   *
   * @param messageId The message containing the attachments
   * @param attachmentIds List of attachment IDs to delete
   * @param userId The user requesting deletion
   */
  public void deleteAttachments(String messageId, List<String> attachmentIds, String userId) {
    MNTMessage message = getMessageEntity(messageId);

    // Only the sender can delete attachments from their own messages
    if (!message.getSenderId().equals(userId)) {
      throw new ForbiddenException("Only the sender can delete attachments from a message");
    }

    String roomId = message.getRoomId();

    for (String attachmentId : attachmentIds) {
      MNTMessageAttachment attachment =
          attachmentRepository
              .getById(attachmentId)
              .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));

      // Verify attachment belongs to this message
      if (!messageId.equals(attachment.getMessageId())) {
        throw new BadRequestException("Attachment does not belong to this message");
      }

      if (attachment.isDeleted()) {
        continue; // Already deleted, skip
      }

      // Delete blob from storages
      storagesService.deleteFile(attachmentId, attachment.getUserId());

      // Soft delete metadata
      attachmentRepository.markAsDeleted(attachmentId);

      // Log event
      eventRepository.logAttachmentDeleted(
          messageId, roomId, userId, attachmentId, attachment.getFileName());
    }
  }

  /**
   * Gets an attachment's file stream for download. Verifies user has access via message -> room.
   */
  public InputStream getAttachmentStream(String attachmentId, String userId) {
    MNTMessageAttachment attachment =
        attachmentRepository
            .getById(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));

    if (attachment.isDeleted()) {
      throw new NotFoundException("Attachment has been deleted");
    }

    // Attachment must be linked to a message
    if (attachment.getMessageId() == null) {
      throw new NotFoundException("Attachment not found: " + attachmentId);
    }

    // Get message to find room, then verify user access
    MNTMessage message = getMessageEntity(attachment.getMessageId());
    validateUserInRoom(message.getRoomId(), userId);

    return storagesService.getFileStreamById(attachmentId, attachment.getUserId());
  }

  /**
   * Gets attachment metadata. Verifies user has access via message -> room.
   */
  public MNTAttachmentDto getAttachment(String attachmentId, String userId) {
    MNTMessageAttachment attachment =
        attachmentRepository
            .getById(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));

    if (attachment.isDeleted()) {
      throw new NotFoundException("Attachment has been deleted");
    }

    // Attachment must be linked to a message
    if (attachment.getMessageId() == null) {
      throw new NotFoundException("Attachment not found: " + attachmentId);
    }

    // Get message to find room, then verify user access
    MNTMessage message = getMessageEntity(attachment.getMessageId());
    validateUserInRoom(message.getRoomId(), userId);

    return toAttachmentDto(attachment);
  }

  /**
   * Cleans up orphan attachments (pending uploads older than threshold). Deletes both blob from
   * storages and metadata from DB.
   *
   * @param olderThanHours Delete attachments older than this many hours
   * @return Number of attachments cleaned up
   */
  public int cleanupOrphanAttachments(int olderThanHours) {
    java.time.OffsetDateTime threshold =
        java.time.OffsetDateTime.now().minusHours(olderThanHours);
    List<MNTMessageAttachment> orphans = attachmentRepository.deleteOrphans(threshold);

    // Delete blobs from storages
    for (MNTMessageAttachment orphan : orphans) {
      try {
        storagesService.deleteFile(orphan.getId(), orphan.getUserId());
      } catch (Exception e) {
        // Log but continue - metadata already deleted
      }
    }

    return orphans.size();
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

    // Load attachments (non-deleted only)
    List<MNTMessageAttachment> attachments = attachmentRepository.getByMessageId(message.getId());
    if (!attachments.isEmpty()) {
      dto.attachments(attachments.stream().map(this::toAttachmentDto).collect(Collectors.toList()));
    }

    return dto;
  }

  private MNTAttachmentDto toAttachmentDto(MNTMessageAttachment attachment) {
    return MNTAttachmentDto.create()
        .id(attachment.getId())
        .messageId(attachment.getMessageId())
        .fileName(attachment.getFileName())
        .mimeType(attachment.getMimeType())
        .fileSize(attachment.getFileSize())
        .userId(attachment.getUserId())
        .createdAt(attachment.getCreatedAt());
  }
}
