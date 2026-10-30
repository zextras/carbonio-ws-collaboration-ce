// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.UserContactRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@UnitTest
class MessageDispatcherImplTest {

  private static final String ROOM = "room";
  private static final String USER = "user";
  private static final String FILE = "file";

  private final MessageWriteRepository messageWriteRepository = mock(MessageWriteRepository.class);
  private final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);
  private final MessageReceivedBroadcaster broadcaster = mock(MessageReceivedBroadcaster.class);
  private final MessageRepository messageRepository = mock(MessageRepository.class);
  private final MessageDispatcherImpl dispatcher =
      new MessageDispatcherImpl(
          mock(ChatMessageService.class),
          messageWriteRepository,
          mock(UserContactRepository.class),
          new JacksonConfig().get(),
          messageRepository,
          fileMetadataRepository,
          broadcaster,
          new TimelineItemMapper(new JacksonConfig().get()));

  private Message send(String replyId) {
    return dispatcher.sendAttachment(
        ROOM, USER, FILE, "f.png", "image/png", 10L, "", replyId, null, null);
  }

  @Test
  void savesTheMetadataBeforeTheMessageIsInsertedAndBroadcast() throws Exception {
    when(messageWriteRepository.sendMessage(
            anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(
            Optional.of(
                new MessageWriteRepository.SendResult(
                    "m", OffsetDateTime.parse("2026-01-01T10:00:00Z"))));

    Message sent = send(null);

    InOrder order = inOrder(fileMetadataRepository, messageWriteRepository, broadcaster);
    ArgumentCaptor<FileMetadata> metadata = ArgumentCaptor.forClass(FileMetadata.class);
    order.verify(fileMetadataRepository).save(metadata.capture());
    order
        .verify(messageWriteRepository)
        .sendMessage(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    order.verify(broadcaster).broadcast(anyString(), any(), any());
    assertEquals(FILE, metadata.getValue().getId());
    assertEquals(ROOM, metadata.getValue().getRoomId());
    assertEquals(sent.getId(), metadata.getValue().getMessageId());
  }

  @Test
  void deletesTheMetadataWhenTheSendFailsOnReplyTarget() throws Exception {
    when(messageWriteRepository.sendMessage(
            anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> send("reply"));

    verify(fileMetadataRepository).delete(any(FileMetadata.class));
    verify(broadcaster, never()).broadcast(anyString(), any(), any());
  }

  @Test
  void deletesTheMetadataWhenTheSenderIsNotAMember() {
    when(messageWriteRepository.sendMessage(
            anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(Optional.empty());

    assertThrows(ForbiddenException.class, () -> send(null));

    verify(fileMetadataRepository).delete(any(FileMetadata.class));
  }

  private TimelineMessageDto broadcastReplyingTo(Message replied) throws Exception {
    when(messageWriteRepository.sendMessage(
            anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(
            Optional.of(
                new MessageWriteRepository.SendResult(
                    "m", OffsetDateTime.parse("2026-01-01T10:00:00Z"))));
    when(messageRepository.getById(replied.getId())).thenReturn(Optional.of(replied));

    send(replied.getId());

    ArgumentCaptor<TimelineMessageDto> sent = ArgumentCaptor.forClass(TimelineMessageDto.class);
    verify(broadcaster).broadcast(eq(ROOM), sent.capture(), isNull());
    assertEquals(replied.getId(), sent.getValue().getReplyToId());
    assertNotNull(sent.getValue().getRepliedMessage());
    return sent.getValue().getRepliedMessage();
  }

  @Test
  void embedsTheEditedInfoOfTheRepliedMessage() throws Exception {
    OffsetDateTime editedAt = OffsetDateTime.parse("2026-01-01T09:30:00Z");
    Message replied = mock(Message.class);
    when(replied.getId()).thenReturn("edited");
    when(replied.getRoomId()).thenReturn(ROOM);
    when(replied.getSenderId()).thenReturn("other");
    when(replied.getText()).thenReturn("new text");
    when(replied.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-01-01T09:00:00Z"));
    when(replied.isEdited()).thenReturn(true);
    when(replied.getUpdatedAt()).thenReturn(editedAt);

    TimelineMessageDto repliedDto = broadcastReplyingTo(replied);

    assertEquals(editedAt, repliedDto.getEditedInfo().getEditedAt());
    assertEquals("new text", repliedDto.getText());
  }

  @Test
  void embedsTheDeletedInfoOfTheRepliedMessage() throws Exception {
    OffsetDateTime deletedAt = OffsetDateTime.parse("2026-01-01T09:30:00Z");
    Message replied =
        Message.create()
            .id("deleted")
            .roomId(ROOM)
            .senderId("other")
            .text("")
            .deleted(true)
            .deletedAt(deletedAt)
            .createdAt(OffsetDateTime.parse("2026-01-01T09:00:00Z"));

    TimelineMessageDto repliedDto = broadcastReplyingTo(replied);

    assertEquals("other", repliedDto.getDeletedInfo().getDeletedBy());
    assertEquals(deletedAt, repliedDto.getDeletedInfo().getDeletedAt());
  }

  @Test
  void embedsTheForwardedInfoOfTheRepliedMessage() throws Exception {
    OffsetDateTime originalSentAt = OffsetDateTime.parse("2025-12-31T08:00:00Z");
    Message replied =
        Message.create()
            .id("forwarded")
            .roomId(ROOM)
            .senderId("forwarder")
            .text("hello")
            .forwarded(true)
            .forwardedSenderId("author")
            .forwardedAt(originalSentAt)
            .createdAt(OffsetDateTime.parse("2026-01-01T09:00:00Z"));

    TimelineMessageDto repliedDto = broadcastReplyingTo(replied);

    assertEquals("author", repliedDto.getForwardedInfo().getOriginalSenderId());
    assertEquals(originalSentAt, repliedDto.getForwardedInfo().getOriginalSentAt());
  }
}
