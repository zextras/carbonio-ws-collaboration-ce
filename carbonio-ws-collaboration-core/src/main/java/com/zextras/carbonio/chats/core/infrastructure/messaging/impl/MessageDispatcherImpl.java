// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.UserContactRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Native implementation of MessageDispatcher that stores messages directly in the database and
 * broadcasts events via RabbitMQ to connected WebSocket clients, replacing MongooseIM.
 */
@Singleton
public class MessageDispatcherImpl implements MessageDispatcher {

  private final ChatMessageService chatMessageService;
  private final MessageWriteRepository messageWriteRepository;
  private final UserContactRepository userContactRepository;
  private final ObjectMapper objectMapper;
  private final MessageRepository messageRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final MessageReceivedBroadcaster messageReceivedBroadcaster;
  private final TimelineItemMapper timelineItemMapper;

  @Inject
  public MessageDispatcherImpl(
      ChatMessageService chatMessageService,
      MessageWriteRepository messageWriteRepository,
      UserContactRepository userContactRepository,
      ObjectMapper objectMapper,
      MessageRepository messageRepository,
      FileMetadataRepository fileMetadataRepository,
      MessageReceivedBroadcaster messageReceivedBroadcaster,
      TimelineItemMapper timelineItemMapper) {
    this.chatMessageService = chatMessageService;
    this.messageWriteRepository = messageWriteRepository;
    this.userContactRepository = userContactRepository;
    this.objectMapper = objectMapper;
    this.messageRepository = messageRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.messageReceivedBroadcaster = messageReceivedBroadcaster;
    this.timelineItemMapper = timelineItemMapper;
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  @Override
  public void addUsersToContacts(String user1id, String user2id) {
    chatMessageService.addMutualContact(user1id, user2id);
  }

  @Override
  public Message sendAttachment(
      String roomId,
      String senderId,
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      String description,
      @Nullable String replyId,
      @Nullable String area,
      @Nullable String tempId) {
    String text = description != null ? description : "";

    String newMessageId = UUID.randomUUID().toString();
    FileMetadata metadata =
        FileMetadata.create()
            .id(fileId)
            .name(fileName)
            .originalSize(originalSize)
            .mimeType(mimeType)
            .type(FileMetadataType.ATTACHMENT)
            .userId(senderId)
            .roomId(roomId)
            .messageId(newMessageId)
            .area(area);
    fileMetadataRepository.save(metadata);
    MessageWriteRepository.SendResult sent;
    try {
      sent =
          messageWriteRepository
              .sendMessage(roomId, senderId, text, newMessageId, replyId, fileId)
              .orElseThrow(
                  () ->
                      replyId != null
                          ? new NotFoundException(
                              String.format("Reply target '%s' not found", replyId))
                          : new ForbiddenException(
                              String.format(
                                  "User '%s' is not a member of room '%s'", senderId, roomId)));
    } catch (RuntimeException e) {
      fileMetadataRepository.delete(metadata);
      throw e;
    }
    Message message =
        Message.create()
            .id(newMessageId)
            .roomId(roomId)
            .senderId(senderId)
            .text(text)
            .replyToId(replyId)
            .attachmentId(fileId)
            .createdAt(sent.createdAt());

    try {
      Map<String, FileMetadata> attachmentsById = new HashMap<>();
      attachmentsById.put(fileId, metadata);
      TimelineMessageDto messageDto =
          timelineItemMapper.baseMessageFields(message, attachmentsById);
      if (replyId != null) {
        messageRepository
            .getById(replyId)
            .ifPresent(
                replied -> {
                  if (replied.getAttachmentId() != null) {
                    fileMetadataRepository
                        .getById(replied.getAttachmentId())
                        .ifPresent(meta -> attachmentsById.put(meta.getId(), meta));
                  }
                  messageDto.repliedMessage(
                      timelineItemMapper.baseMessageFields(replied, attachmentsById));
                });
      }
      messageReceivedBroadcaster.broadcast(roomId, messageDto, tempId);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting attachment message: " + e.getMessage());
    }
    return message;
  }

  @Override
  public Optional<String> getAttachmentIdFromMessage(String message) {
    if (message != null && message.contains("\"attachmentId\"")) {
      try {
        var node = objectMapper.readTree(message);
        var attachmentNode = node.get("attachmentId");
        if (attachmentNode != null && !attachmentNode.isNull()) {
          return Optional.of(attachmentNode.asText());
        }
      } catch (Exception e) {
        // Fall through
      }
    }
    if (message != null && message.contains("<attachment-id>")) {
      try {
        int startIdx = message.indexOf("<attachment-id>");
        int endIdx = message.indexOf("</attachment-id>");
        if (startIdx >= 0 && endIdx > startIdx) {
          return Optional.of(message.substring(startIdx + 15, endIdx));
        }
      } catch (Exception e) {
        // Fall through
      }
    }
    return Optional.empty();
  }
}
