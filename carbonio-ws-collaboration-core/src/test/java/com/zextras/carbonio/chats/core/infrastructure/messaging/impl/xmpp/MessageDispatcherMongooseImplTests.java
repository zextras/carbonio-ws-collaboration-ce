// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
public class MessageDispatcherMongooseImplTests {

  private final MessageDispatcherMongooseImpl messageDispatcherMongooseImpl;

  public MessageDispatcherMongooseImplTests() {
    messageDispatcherMongooseImpl = new MessageDispatcherMongooseImpl(
      "mongooseimUrl", "username", "password", new ObjectMapper()
    );
  }

  @Test
  @DisplayName("Given an XMPP message that contains information about an attachment, it correctly returns its id")
  public void getAttachmentIdFromMessageTests_ok() {
    UUID attachmentId = UUID.randomUUID();
    String message =
      "<message from='userJid/roomJid' to='roomJid' id='messageId' type='groupchat' xmlns='jabber:client'>"
        + "  <x xmlns='urn:xmpp:muclight:0#configuration'>"
        + "    <operation>attachmentAdded</operation>"
        + "    <attachment-id>" + attachmentId + "</attachment-id>"
        + "    <filename>filename</filename>"
        + "    <mime-type>mimeType</mime-type>"
        + "    <size>1024</size>"
        + "  </x>"
        + "</message>";
    assertEquals(attachmentId.toString(),
      messageDispatcherMongooseImpl.getAttachmentIdFromMessage(message).orElseThrow());
  }


  @Test
  @DisplayName("Given a simple XMPP message, it correctly returns an empty Optional")
  public void getAttachmentIdFromMessageTests_simpleMessage() {
    String message =
      "<message from='userJid/roomJid' to='roomJid' id='messageId' type='groupchat' xmlns='jabber:client'>"
        + "  <body>text message</body>"
        + "</message>";
    assertEquals(Optional.empty(), messageDispatcherMongooseImpl.getAttachmentIdFromMessage(message));
  }

  @Test
  @DisplayName("Given an XMPP message from MAM that contains information about an attachment, it correctly returns its id")
  public void getAttachmentIdFromMessageTests_simpleMessageFromMam() {
    String message = "<message from='userJid/roomJid' id='messageId' type='groupchat' xmlns='jabber:client'>"
      + "  <body>text message</body>"
      + "  <x xmlns='http://jabber.org/protocol/muc#user'>"
      + "    <item affiliation='member' jid='userJid/affiliationJid' role='participant'></item>"
      + "  </x>"
      + "</message>";
    assertEquals(Optional.empty(), messageDispatcherMongooseImpl.getAttachmentIdFromMessage(message));
  }

  @Test
  @DisplayName("Given an XMPP message that contains information about an attachment, it correctly returns its id")
  public void getAttachmentIdFromMessageTests_ppp() {
    UUID attachmentId = UUID.randomUUID();
    String message =
      "<message from='userJid/roomJid' to='roomJid' id='messageId' type='groupchat' xmlns='jabber:client'>"
        + "  <x xmlns='urn:xmpp:muclight:0#configuration'>"
        + "    <operation>attachmentAdded</operation>"
        + "    <attachment-id></attachment-id>"
        + "  </x>"
        + "</message>";
    assertEquals(Optional.empty(), messageDispatcherMongooseImpl.getAttachmentIdFromMessage(message));
  }
}
