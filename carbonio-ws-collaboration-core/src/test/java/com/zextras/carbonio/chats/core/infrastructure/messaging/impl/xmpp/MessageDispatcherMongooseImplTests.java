// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcherClient;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.MessageDispatcherMongooseImpl;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlBody;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlResponse;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.StanzaResponse;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MessageDispatcherMongooseImplTests {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final MessageDispatcherClient messageDispatcherClient;
  private final MessageDispatcherMongooseImpl messageDispatcherMongooseImpl;

  public MessageDispatcherMongooseImplTests() {
    this.messageDispatcherClient = mock(MessageDispatcherClient.class);
    messageDispatcherMongooseImpl = new MessageDispatcherMongooseImpl(messageDispatcherClient);
  }

  private GraphQlResponse graphQlResponse(String json) throws Exception {
    return OBJECT_MAPPER.readValue(json, GraphQlResponse.class);
  }

  private GraphQlResponse successResponse() throws Exception {
    return graphQlResponse("{\"data\":{\"stanza\":{\"sendStanza\":{\"id\":\"s1\"}}}}");
  }

  @Nested
  @DisplayName("sendAttachment tests")
  class SendAttachmentTests {

    @Test
    @DisplayName("Returns the message id and stanza_id assigned by MongooseIM on success")
    void sendAttachment_returnsStanzaResponse() throws Exception {
      String messageId = UUID.randomUUID().toString();
      String stanzaId = UUID.randomUUID().toString();
      when(messageDispatcherClient.executeMutation(any()))
          .thenReturn(
              graphQlResponse(
                  String.format(
                      "{\"data\":{\"stanza\":{\"sendStanza\":{\"id\":\"%s\",\"stanza_id\":\"%s\"}}}}",
                      messageId, stanzaId)));

      StanzaResponse result =
          messageDispatcherMongooseImpl.sendAttachment(
              "room-1",
              "user-1",
              "file-1",
              "test.pdf",
              "application/pdf",
              1024L,
              "description",
              null,
              null,
              null);

      assertAll(
          () -> assertEquals(messageId, result.id()),
          () -> assertEquals(stanzaId, result.stanzaId()));
    }

    @Test
    @DisplayName("Returns null fields when MongooseIM response omits id and stanza_id")
    void sendAttachment_returnsNullFieldsWhenMissing() throws Exception {
      when(messageDispatcherClient.executeMutation(any()))
          .thenReturn(graphQlResponse("{\"data\":{\"stanza\":{\"sendStanza\":{}}}}"));

      StanzaResponse result =
          messageDispatcherMongooseImpl.sendAttachment(
              "room-1",
              "user-1",
              "file-1",
              "test.pdf",
              "application/pdf",
              1024L,
              "description",
              null,
              null,
              null);

      assertAll(() -> assertNull(result.id()), () -> assertNull(result.stanzaId()));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when MongooseIM returns errors")
    void sendAttachment_throwsOnErrors() {
      when(messageDispatcherClient.executeMutation(any()))
          .thenThrow(new MessageDispatcherException("stanza error"));

      assertThrows(
          MessageDispatcherException.class,
          () ->
              messageDispatcherMongooseImpl.sendAttachment(
                  "room-1",
                  "user-1",
                  "file-1",
                  "test.pdf",
                  "application/pdf",
                  1024L,
                  "description",
                  null,
                  null,
                  null));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when the client throws")
    void sendAttachment_throwsOnClientException() {
      when(messageDispatcherClient.executeMutation(any()))
          .thenThrow(new MessageDispatcherException("HTTP error"));

      assertThrows(
          MessageDispatcherException.class,
          () ->
              messageDispatcherMongooseImpl.sendAttachment(
                  "room-1",
                  "user-1",
                  "file-1",
                  "test.pdf",
                  "application/pdf",
                  1024L,
                  "description",
                  null,
                  null,
                  null));
    }

    @Test
    @DisplayName("Stanza includes attachment metadata (fileId, fileName, mimeType, size)")
    void sendAttachment_stanzaContainsAttachmentMetadata() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      messageDispatcherMongooseImpl.sendAttachment(
          "room-1",
          "user-1",
          "file-42",
          "report.pdf",
          "application/pdf",
          2048L,
          "my description",
          null,
          null,
          null);

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      String query = captor.getValue().getQuery();
      assertAll(
          () -> assertTrue(query.contains("file-42"), "stanza must carry attachment id"),
          () -> assertTrue(query.contains("report.pdf"), "stanza must carry filename"),
          () -> assertTrue(query.contains("application/pdf"), "stanza must carry mime-type"),
          () -> assertTrue(query.contains("2048"), "stanza must carry size"));
    }

    @Test
    @DisplayName("Stanza carries messageId as message id attribute when provided")
    void sendAttachment_stanzaContainsMessageId() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      messageDispatcherMongooseImpl.sendAttachment(
          "room-1",
          "user-1",
          "file-1",
          "test.pdf",
          "application/pdf",
          1024L,
          "desc",
          "msg-id-abc",
          null,
          null);

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      assertTrue(captor.getValue().getQuery().contains("msg-id-abc"));
    }

    @Test
    @DisplayName("Stanza carries replyId when provided")
    void sendAttachment_stanzaContainsReplyId() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      messageDispatcherMongooseImpl.sendAttachment(
          "room-1",
          "user-1",
          "file-1",
          "test.pdf",
          "application/pdf",
          1024L,
          "desc",
          null,
          "reply-id-xyz",
          null);

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      assertTrue(captor.getValue().getQuery().contains("reply-id-xyz"));
    }

    @Test
    @DisplayName("Stanza carries area config when provided")
    void sendAttachment_stanzaContainsArea() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      messageDispatcherMongooseImpl.sendAttachment(
          "room-1",
          "user-1",
          "file-1",
          "test.pdf",
          "application/pdf",
          1024L,
          "desc",
          null,
          null,
          "image-area-value");

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      assertTrue(captor.getValue().getQuery().contains("image-area-value"));
    }
  }

  @Nested
  @DisplayName("forwardMessage tests")
  class ForwardMessageTests {

    private static final String ORIGINAL_MESSAGE =
        "<message xmlns=\"jabber:client\" from=\"user-1@carbonio\""
            + " to=\"room-1@muclight.carbonio\" type=\"groupchat\">"
            + "<body>hello</body></message>";

    private ForwardMessageDto buildDto() {
      return ForwardMessageDto.create()
          .originalMessage(ORIGINAL_MESSAGE)
          .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
          .description("forwarded");
    }

    @Test
    @DisplayName("Forwards a plain text message without throwing")
    void forwardMessage_textMessage() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      assertDoesNotThrow(
          () -> messageDispatcherMongooseImpl.forwardMessage("room-1", "user-1", buildDto(), null));
    }

    @Test
    @DisplayName("Forwards an attachment message including messageId and stanzaId in the stanza")
    void forwardMessage_attachmentMessage_includesMessageIdAndStanzaId() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      FileMetadata metadata =
          FileMetadata.create()
              .id(UUID.randomUUID().toString())
              .name("image.jpg")
              .mimeType("image/jpg")
              .originalSize(1024L)
              .messageId("msg-id-xyz")
              .stanzaId("stanza-id-xyz");

      messageDispatcherMongooseImpl.forwardMessage("room-1", "user-1", buildDto(), metadata);

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      String query = captor.getValue().getQuery();
      assertAll(
          () -> assertTrue(query.contains("msg-id-xyz"), "stanza must carry messageId"),
          () -> assertTrue(query.contains("stanza-id-xyz"), "stanza must carry stanzaId"));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when MongooseIM returns errors on forward")
    void forwardMessage_throwsOnErrors() {
      when(messageDispatcherClient.executeMutation(any()))
          .thenThrow(new MessageDispatcherException("fail"));

      assertThrows(
          MessageDispatcherException.class,
          () -> messageDispatcherMongooseImpl.forwardMessage("room-1", "user-1", buildDto(), null));
    }
  }

  @Nested
  @DisplayName("sendMeetingDeclined tests")
  class SendMeetingDeclinedTests {

    @Test
    @DisplayName("Sends stanza containing meetingDeclined operation without throwing on success")
    void sendMeetingDeclined_sendsCorrectStanza() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      assertDoesNotThrow(
          () -> messageDispatcherMongooseImpl.sendMeetingDeclined("room-1", "user-1"));

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      assertTrue(captor.getValue().getQuery().contains("meetingDeclined"));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when MongooseIM returns errors")
    void sendMeetingDeclined_throwsOnErrors() {
      when(messageDispatcherClient.executeMutation(any()))
          .thenThrow(new MessageDispatcherException("fail"));

      assertThrows(
          MessageDispatcherException.class,
          () -> messageDispatcherMongooseImpl.sendMeetingDeclined("room-1", "user-1"));
    }
  }

  @Nested
  @DisplayName("sendMeetingStarted tests")
  class SendMeetingStartedTests {

    @Test
    @DisplayName("Sends stanza containing meetingStarted operation without throwing on success")
    void sendMeetingStarted_sendsCorrectStanza() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());

      assertDoesNotThrow(
          () -> messageDispatcherMongooseImpl.sendMeetingStarted("room-1", "user-1"));

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      assertTrue(captor.getValue().getQuery().contains("meetingStarted"));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when MongooseIM returns errors")
    void sendMeetingStarted_throwsOnErrors() {
      when(messageDispatcherClient.executeMutation(any()))
          .thenThrow(new MessageDispatcherException("fail"));

      assertThrows(
          MessageDispatcherException.class,
          () -> messageDispatcherMongooseImpl.sendMeetingStarted("room-1", "user-1"));
    }
  }

  @Nested
  @DisplayName("sendMeetingEnded tests")
  class SendMeetingEndedTests {

    @Test
    @DisplayName("Sends stanza containing meetingEnded operation with duration and startedAt")
    void sendMeetingEnded_sendsCorrectStanza() throws Exception {
      when(messageDispatcherClient.executeMutation(any())).thenReturn(successResponse());
      OffsetDateTime startedAt = OffsetDateTime.parse("2024-01-01T10:00:00Z");

      assertDoesNotThrow(
          () ->
              messageDispatcherMongooseImpl.sendMeetingEnded("room-1", "user-1", startedAt, 300L));

      ArgumentCaptor<GraphQlBody> captor = ArgumentCaptor.forClass(GraphQlBody.class);
      verify(messageDispatcherClient).executeMutation(captor.capture());
      String query = captor.getValue().getQuery();
      assertAll(
          () ->
              assertTrue(
                  query.contains("meetingEnded"), "stanza must carry meetingEnded operation"),
          () -> assertTrue(query.contains("300"), "stanza must carry duration"),
          () ->
              assertTrue(
                  query.contains(String.valueOf(startedAt.toInstant().toEpochMilli())),
                  "stanza must carry startedAt epoch millis"));
    }

    @Test
    @DisplayName("Throws MessageDispatcherException when MongooseIM returns errors")
    void sendMeetingEnded_throwsOnErrors() {
      when(messageDispatcherClient.executeMutation(any()))
          .thenThrow(new MessageDispatcherException("fail"));

      assertThrows(
          MessageDispatcherException.class,
          () ->
              messageDispatcherMongooseImpl.sendMeetingEnded(
                  "room-1", "user-1", OffsetDateTime.parse("2024-01-01T10:00:00Z"), 300L));
    }
  }
}
