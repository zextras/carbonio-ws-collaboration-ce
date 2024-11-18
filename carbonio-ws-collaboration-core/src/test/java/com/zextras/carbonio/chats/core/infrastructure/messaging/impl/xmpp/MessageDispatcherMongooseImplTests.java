// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class MessageDispatcherMongooseImplTests {

  private final MessageDispatcherMongooseImpl messageDispatcherMongooseImpl;

  public MessageDispatcherMongooseImplTests() {
    messageDispatcherMongooseImpl =
        new MessageDispatcherMongooseImpl(
            mock(HttpClient.class), "mongooseimUrl", "token", new ObjectMapper());
  }

  @Test
  @DisplayName(
      "Given an XMPP message that contains information about an attachment, it correctly returns"
          + " its id")
  void getAttachmentIdFromMessageTests_ok() {
    UUID attachmentId = UUID.randomUUID();
    String message =
        "<message from='userJid/roomJid' to='roomJid' id='messageId' type='groupchat'"
            + " xmlns='jabber:client'>  <x xmlns='urn:xmpp:muclight:0#configuration'>   "
            + " <operation>attachmentAdded</operation>    <attachment-id>"
            + attachmentId
            + "</attachment-id>"
            + "    <filename>filename</filename>"
            + "    <mime-type>mimeType</mime-type>"
            + "    <size>1024</size>"
            + "  </x>"
            + "</message>";
    assertEquals(
        attachmentId.toString(),
        messageDispatcherMongooseImpl.getAttachmentIdFromMessage(message).orElseThrow());
  }

  @Test
  @DisplayName("Given a simple XMPP message, it correctly returns an empty Optional")
  void getAttachmentIdFromMessageTests_simpleMessage() {
    String message =
        "<message from='userJid/roomJid' to='roomJid' id='messageId' type='groupchat'"
            + " xmlns='jabber:client'>  <body>text message</body></message>";
    assertEquals(
        Optional.empty(), messageDispatcherMongooseImpl.getAttachmentIdFromMessage(message));
  }
}
