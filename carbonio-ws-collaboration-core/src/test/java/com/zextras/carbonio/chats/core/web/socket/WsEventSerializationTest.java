// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.async.model.MessageDeleted;
import com.zextras.carbonio.async.model.MessageEdited;
import com.zextras.carbonio.async.model.MessageReceived;
import com.zextras.carbonio.async.model.WsError;
import com.zextras.carbonio.async.model.WsPong;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WsEventSerializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  void messageReceived_withTempId_containsAllFields() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    TimelineMessageDto msg =
        TimelineMessageDto.create()
            .id("msg-1")
            .roomId("room-1")
            .senderId("user-1")
            .text("hello")
            .createdAt(sentDate);

    String json =
        objectMapper.writeValueAsString(
            MessageReceived.create().message(msg).tempId("tmp-abc").sentDate(sentDate));

    assertThat(json).contains("\"type\":\"MessageReceived\"");
    assertThat(json).contains("\"sentDate\":");
    assertThat(json).contains("\"message\":");
    assertThat(json).contains("\"tempId\":\"tmp-abc\"");
    assertThat(json).doesNotContain("\"requestId\"");
  }

  @Test
  void messageReceived_withoutTempId_tempIdAbsent() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    TimelineMessageDto msg =
        TimelineMessageDto.create()
            .id("msg-1")
            .roomId("room-1")
            .senderId("user-1")
            .text("hello")
            .createdAt(sentDate);

    String json =
        objectMapper.writeValueAsString(MessageReceived.create().message(msg).sentDate(sentDate));

    assertThat(json).contains("\"type\":\"MessageReceived\"");
    assertThat(json).doesNotContain("\"tempId\"");
    assertThat(json).doesNotContain("\"requestId\"");
  }

  @Test
  void messageEdited_containsExpectedFields() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime editedAt = OffsetDateTime.of(2025, 1, 1, 12, 5, 0, 0, ZoneOffset.UTC);

    String json =
        objectMapper.writeValueAsString(
            MessageEdited.create()
                .messageId("msg-1")
                .roomId("room-1")
                .text("edited text")
                .editedAt(editedAt)
                .sentDate(sentDate));

    assertThat(json).contains("\"type\":\"MessageEdited\"");
    assertThat(json).contains("\"messageId\":\"msg-1\"");
    assertThat(json).contains("\"roomId\":\"room-1\"");
    assertThat(json).contains("\"text\":\"edited text\"");
    assertThat(json).contains("\"editedAt\":");
    assertThat(json).doesNotContain("\"requestId\"");
  }

  @Test
  void messageDeleted_containsDeletedBy() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    String json =
        objectMapper.writeValueAsString(
            MessageDeleted.create()
                .messageId("msg-1")
                .roomId("room-1")
                .deletedBy("user-2")
                .deletedAt(sentDate)
                .sentDate(sentDate));

    assertThat(json).contains("\"type\":\"MessageDeleted\"");
    assertThat(json).contains("\"deletedBy\":\"user-2\"");
    assertThat(json).doesNotContain("\"requestId\"");
  }

  @Test
  void wsPong_containsTypeAndSentDate() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    WsPong pong = WsPong.create();
    pong.setSentDate(sentDate);

    String json = objectMapper.writeValueAsString(pong);

    assertThat(json).contains("\"type\":\"Pong\"");
    assertThat(json).contains("\"sentDate\":");
    assertThat(json).doesNotContain("\"message\"");
    assertThat(json).doesNotContain("\"requestId\"");
    assertThat(json).doesNotContain("\"code\"");
  }

  @Test
  void wsError_withRequiredMessageOnly_containsTypeAndSentDateAndMessage() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    WsError error = WsError.create();
    error.setSentDate(sentDate);
    error.setMessage("Something went wrong");

    String json = objectMapper.writeValueAsString(error);

    assertThat(json).contains("\"type\":\"Error\"");
    assertThat(json).contains("\"sentDate\":");
    assertThat(json).contains("\"message\":\"Something went wrong\"");
  }

  @Test
  void wsError_withRequestIdAndCode_containsAllFields() throws Exception {
    OffsetDateTime sentDate = OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    WsError error = WsError.create();
    error.setSentDate(sentDate);
    error.setMessage("Forbidden");
    error.setRequestId("req-42");
    error.setCode("ERR_FORBIDDEN");

    String json = objectMapper.writeValueAsString(error);

    assertThat(json).contains("\"type\":\"Error\"");
    assertThat(json).contains("\"sentDate\":");
    assertThat(json).contains("\"message\":\"Forbidden\"");
    assertThat(json).contains("\"requestId\":\"req-42\"");
    assertThat(json).contains("\"code\":\"ERR_FORBIDDEN\"");
  }
}
