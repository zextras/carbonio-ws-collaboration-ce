// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.time.OffsetDateTime;

@Singleton
public class AttachmentAccessValidator {

  private final ChatMessageService chatMessageService;
  private final MessageRepository messageRepository;

  @Inject
  public AttachmentAccessValidator(
      ChatMessageService chatMessageService, MessageRepository messageRepository) {
    this.chatMessageService = chatMessageService;
    this.messageRepository = messageRepository;
  }

  public void validate(FileMetadata metadata, UserPrincipal user) {
    VisibilityFloor floor =
        chatMessageService.getVisibilityFloor(metadata.getRoomId(), user.getId());
    if (floor.clearedAt() == null) {
      return;
    }
    OffsetDateTime createdAt = creationInstant(metadata);
    if (createdAt != null && !floor.canAccess(createdAt)) {
      throw new NotFoundException(String.format("Attachment '%s' not found", metadata.getId()));
    }
  }

  private OffsetDateTime creationInstant(FileMetadata metadata) {
    if (metadata.getMessageId() == null) {
      return metadata.getCreatedAt();
    }
    return messageRepository
        .getById(metadata.getMessageId())
        .map(Message::getCreatedAt)
        .orElse(metadata.getCreatedAt());
  }
}
