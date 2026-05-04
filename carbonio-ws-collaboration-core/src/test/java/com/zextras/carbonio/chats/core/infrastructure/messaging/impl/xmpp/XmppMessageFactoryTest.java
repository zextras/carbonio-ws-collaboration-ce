// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class XmppMessageFactoryTest {

  private final XmppMessageFactory factory = new XmppMessageFactory();

  @Test
  @DisplayName("Builds a meetingDeclined stanza with correct JIDs, type, operation and timestamp")
  void buildMeetingDeclineMessage_testOk() {
    String result = factory.buildMeetingDeclineMessage("room-id", "sender-id");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>meetingDeclined</operation>"));
    assertTrue(result.matches(".*<timestamp>\\d+</timestamp>.*"));
  }

  @Test
  @DisplayName("Builds a meetingStarted stanza with correct JIDs, type, operation and timestamp")
  void buildMeetingStartMessage_testOk() {
    String result = factory.buildMeetingStartMessage("room-id", "sender-id");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>meetingStarted</operation>"));
    assertTrue(result.matches(".*<timestamp>\\d+</timestamp>.*"));
  }

  @Test
  @DisplayName(
      "Builds a meetingEnded stanza with correct JIDs, type, operation, timestamp, startedAt and"
          + " duration")
  void buildMeetingEndedMessage_testOk() {
    OffsetDateTime startedAt = OffsetDateTime.now().minusMinutes(5);
    String result = factory.buildMeetingEndedMessage("room-id", "sender-id", startedAt, 300);

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>meetingEnded</operation>"));
    assertTrue(result.matches(".*<timestamp>\\d+</timestamp>.*"));
    assertTrue(
        result.contains("<startedAt>" + startedAt.toInstant().toEpochMilli() + "</startedAt>"));
    assertTrue(result.contains("<duration>300</duration>"));
  }

  @Test
  @DisplayName("Builds a roomNameChanged stanza with correct JIDs, type, operation and value")
  void buildRoomNameChangedMessage_testOk() {
    String result = factory.buildRoomNameChangedMessage("room-id", "sender-id", "New Room Name");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>roomNameChanged</operation>"));
    assertTrue(result.contains("<value encoded='UTF-8'>New Room Name</value>"));
  }

  @Test
  @DisplayName(
      "Builds a roomDescriptionChanged stanza with correct JIDs, type, operation and value")
  void buildRoomDescriptionChangedMessage_testOk() {
    String result =
        factory.buildRoomDescriptionChangedMessage("room-id", "sender-id", "New description");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>roomDescriptionChanged</operation>"));
    assertTrue(result.contains("<value encoded='UTF-8'>New description</value>"));
  }

  @Test
  @DisplayName(
      "Builds a roomPictureUpdated stanza with correct JIDs, type, operation, picture-id and"
          + " picture-name")
  void buildRoomPictureUpdatedMessage_testOk() {
    String result =
        factory.buildRoomPictureUpdatedMessage("room-id", "sender-id", "abc-123", "avatar.png");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>roomPictureUpdated</operation>"));
    assertTrue(result.contains("<picture-id>abc-123</picture-id>"));
    assertTrue(result.contains("<picture-name encoded='UTF-8'>avatar.png</picture-name>"));
  }

  @Test
  @DisplayName("Builds a roomPictureDeleted stanza with correct JIDs, type and operation")
  void buildRoomPictureDeletedMessage_testOk() {
    String result = factory.buildRoomPictureDeletedMessage("room-id", "sender-id");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>roomPictureDeleted</operation>"));
    assertFalse(result.contains("<picture-id>"));
    assertFalse(result.contains("<picture-name>"));
  }

  @Test
  @DisplayName(
      "Builds a roomHistoryCleared stanza with correct JIDs, type, operation, user-id and"
          + " cleared-at")
  void buildRoomHistoryClearedMessage_testOk() {
    String result =
        factory.buildRoomHistoryClearedMessage("room-id", "sender-id", "2024-01-15T10:30:00Z");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("<operation>roomHistoryCleared</operation>"));
    assertTrue(result.contains("<user-id>sender-id</user-id>"));
    assertTrue(result.contains("<cleared-at>2024-01-15T10:30:00Z</cleared-at>"));
  }

  @Test
  @DisplayName(
      "Builds an attachmentAdded stanza with all parameters including optional messageId, replyId"
          + " and area")
  void buildAttachmentAddedMessage_withAllParams_testOk() {
    String result =
        factory.buildAttachmentAddedMessage(
            "room-id",
            "sender-id",
            "file-uuid",
            "document.pdf",
            "application/pdf",
            204800,
            "Optional description",
            "msg-123",
            "reply-456",
            "meeting-area");

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("type='groupchat'"));
    assertTrue(result.contains("id='msg-123'"));
    assertTrue(result.contains("<operation>attachmentAdded</operation>"));
    assertTrue(result.contains("<attachment-id>file-uuid</attachment-id>"));
    assertTrue(result.contains("<filename encoded='UTF-8'>document.pdf</filename>"));
    assertTrue(result.contains("<mime-type>application/pdf</mime-type>"));
    assertTrue(result.contains("<size>204800</size>"));
    assertTrue(result.contains("<body encoded='UTF-8'>Optional description</body>"));
    assertTrue(result.contains("<reply xmlns='urn:xmpp:reply:0'"));
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("id='reply-456'"));
    assertTrue(result.contains("<area>meeting-area</area>"));
  }

  @Test
  @DisplayName("Builds an attachmentAdded stanza without optional parameters when null")
  void buildAttachmentAddedMessage_withoutOptionalParams_testOk() {
    String result =
        factory.buildAttachmentAddedMessage(
            "room-id",
            "sender-id",
            "file-uuid",
            "document.pdf",
            "application/pdf",
            204800,
            "Optional description",
            null,
            null,
            null);

    assertNotNull(result);
    assertTrue(result.contains("to='room-id@muclight.carbonio'"));
    assertTrue(result.contains("from='sender-id@carbonio'"));
    assertTrue(result.contains("<operation>attachmentAdded</operation>"));
    assertTrue(result.contains("<attachment-id>file-uuid</attachment-id>"));
    assertTrue(result.contains("<filename encoded='UTF-8'>document.pdf</filename>"));
    assertTrue(result.contains("<mime-type>application/pdf</mime-type>"));
    assertTrue(result.contains("<size>204800</size>"));
    assertTrue(result.contains("<body encoded='UTF-8'>Optional description</body>"));
    assertFalse(result.contains("<area>"));
  }

  @Test
  @DisplayName("toRoomJid appends MUC domain to room id")
  void toRoomJid_testOk() {
    assertEquals("room-id@muclight.carbonio", XmppMessageFactory.toRoomJid("room-id"));
  }

  @Test
  @DisplayName("toUserJid appends domain to user id")
  void toUserJid_testOk() {
    assertEquals("user-id@carbonio", XmppMessageFactory.toUserJid("user-id"));
  }
}
