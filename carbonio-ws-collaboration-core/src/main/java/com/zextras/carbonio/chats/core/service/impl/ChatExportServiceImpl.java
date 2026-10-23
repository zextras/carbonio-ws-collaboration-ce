// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.service.ChatExportService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.web.utility.ChatExportFormatter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class ChatExportServiceImpl implements ChatExportService {

  private static final int BATCH = 500;

  private final MessageRepository messageRepository;
  private final ChatMessageService chatMessageService;
  private final FileMetadataRepository fileMetadataRepository;
  private final ProfilingService profilingService;
  private final ChatExportFormatter formatter;

  @Inject
  public ChatExportServiceImpl(
      MessageRepository messageRepository,
      ChatMessageService chatMessageService,
      FileMetadataRepository fileMetadataRepository,
      ProfilingService profilingService,
      ChatExportFormatter formatter) {
    this.messageRepository = messageRepository;
    this.chatMessageService = chatMessageService;
    this.fileMetadataRepository = fileMetadataRepository;
    this.profilingService = profilingService;
    this.formatter = formatter;
  }

  @Override
  public StreamingOutput export(
      UUID roomId, UserPrincipal principal, ZoneId zone, String deletedPlaceholder) {
    String roomIdStr = roomId.toString();
    OffsetDateTime lowerBound =
        chatMessageService.getVisibilityFloor(roomIdStr, principal.getId()).clearedAt();

    return outputStream -> {
      Map<String, String> nameCache = new HashMap<>();
      try (Writer writer =
          new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
        OffsetDateTime cursorTs = null;
        String cursorId = null;
        while (true) {
          List<Message> batch =
              messageRepository.getByRoomIdAfterCursor(
                  roomIdStr, lowerBound, cursorTs, cursorId, BATCH);
          if (batch.isEmpty()) {
            break;
          }
          for (Message m : batch) {
            String senderName =
                nameCache.computeIfAbsent(m.getSenderId(), id -> resolveName(principal, id));
            String attachmentName = null;
            if (m.getAttachmentId() != null) {
              attachmentName =
                  fileMetadataRepository
                      .getById(m.getAttachmentId())
                      .map(f -> f.getName())
                      .orElse("");
            }
            writer.write(
                formatter.formatLine(
                    m.getCreatedAt(),
                    zone,
                    senderName,
                    m.getText(),
                    Boolean.TRUE.equals(m.isDeleted()),
                    attachmentName,
                    deletedPlaceholder));
          }
          writer.flush();
          Message lastMessage = batch.get(batch.size() - 1);
          cursorTs = lastMessage.getCreatedAt();
          cursorId = lastMessage.getId();
          if (batch.size() < BATCH) {
            break;
          }
        }
      } catch (IOException e) {
        throw new WebApplicationException("Chat export streaming failed for room " + roomIdStr, e);
      }
    };
  }

  /** Resolves a sender display name; falls back to the raw id on miss or error (FE parity). */
  private String resolveName(UserPrincipal principal, String senderId) {
    try {
      return profilingService
          .getById(principal, UUID.fromString(senderId))
          .map(p -> p.getName())
          .filter(n -> n != null && !n.isBlank())
          .orElse(senderId);
    } catch (RuntimeException e) {
      ChatsLogger.warn("Export name resolution failed for " + senderId, e);
      return senderId;
    }
  }
}
