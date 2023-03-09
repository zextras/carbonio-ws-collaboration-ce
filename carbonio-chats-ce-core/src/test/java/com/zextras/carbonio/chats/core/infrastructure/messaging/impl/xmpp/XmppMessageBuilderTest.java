// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import java.time.OffsetDateTime;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
public class XmppMessageBuilderTest {

  @Test
  @DisplayName("Builds an XMPP message with body")
  public void buildMessageWithBody() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is my body !</body>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id").body("this is my body !").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message without sender")
  public void buildMessageWithoutSender() {
    ChatsHttpException exception = assertThrows(InternalErrorException.class, () ->
      XmppMessageBuilder.create("recipient-id", null).build());
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getHttpStatusCode());
    assertEquals(Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), exception.getHttpStatusPhrase());
    assertEquals("Internal Server Error - Cannot create an XMPP message without sender", exception.getMessage());
  }

  @Test
  @DisplayName("Builds an XMPP message without recipient")
  public void buildMessageWithoutRecipient() {
    ChatsHttpException exception = assertThrows(InternalErrorException.class, () ->
      XmppMessageBuilder.create(null, "sender-id").build());
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getHttpStatusCode());
    assertEquals(Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), exception.getHttpStatusPhrase());
    assertEquals("Internal Server Error - Cannot create an XMPP message without recipient", exception.getMessage());
  }

  @Test
  @DisplayName("Builds an XMPP message with id")
  public void buildMessageWithId() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" id=\"message-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is my body !</body>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageId("message-id")
      .body("this is my body !").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message with configurations")
  public void buildMessageWithConfigurations() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<x xmlns=\"urn:xmpp:muclight:0#configuration\">"
      + "<config1>option one</config1>"
      + "<config2>option two</config2>"
      + "<config3>option tree</config3></x>"
      + "<body/>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .addConfig("config1", "option one")
      .addConfig("config2", "option two")
      .addConfig("config3", "option tree").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message with configuration type")
  public void buildMessageWithType() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<x xmlns=\"urn:xmpp:muclight:0#configuration\">"
      + "<operation>roomNameChanged</operation>"
      + "<config1>option one</config1>"
      + "</x>"
      + "<body/>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .addConfig("config1", "option one")
      .type(MessageType.ROOM_NAME_CHANGED).build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message with reply")
  public void buildMessageWithReply() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is my body !</body>"
      + "<reply xmlns=\"urn:xmpp:reply:0\" id=\"reply-id\" to=\"recipient-id\"/>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .replyId("reply-id")
      .body("this is my body !").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message that forwards another")
  public void buildMessageWithForward() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is my body !</body>"
      + "<forwarded xmlns=\"urn:xmpp:forward:0\">"
      + "<delay xmlns=\"urn:xmpp:delay\" stamp=\"2023-01-01T00:00:00Z\"/>"
      + "<message from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is the body of the message to forward!</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageToForward("<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
        + "<body>this is the body of the message to forward!</body>"
        + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
      .body("this is my body !").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message that forwards a replied message")
  public void buildMessageWithForwardOfRepliedMessage() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is my body !</body>"
      + "<forwarded xmlns=\"urn:xmpp:forward:0\">"
      + "<delay xmlns=\"urn:xmpp:delay\" stamp=\"2023-01-01T00:00:00Z\"/>"
      + "<message from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is the body of the message to forward!</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageToForward("<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
        + "<body>this is the body of the message to forward!</body>"
        + "<reply xmlns=\"urn:xmpp:reply:0\" id=\"reply-id\" to=\"sender-id\"/>"
        + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
      .body("this is my body !").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message that forwards a forwarded message")
  public void buildMessageWithForwardOfForwardedMessage() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is my body !</body>"
      + "<forwarded xmlns=\"urn:xmpp:forward:0\">"
      + "<delay xmlns=\"urn:xmpp:delay\" stamp=\"2023-01-01T00:00:00Z\"/>"
      + "<message from=\"old-sender-id\" to=\"old-recipient-id\" type=\"groupchat\">"
      + "<body>the forwarded message</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageToForward(
        "<message xmlns=\"jabber:client\" from=\"old-sender-id\" to=\"old-recipient-id\" type=\"groupchat\">"
          + "<body>the forwarded message</body>"
          + "<forwarded xmlns=\"urn:xmpp:forward:0\">"
          + "<delay xmlns=\"urn:xmpp:delay\" stamp=\"2023-01-01T00:00:00Z\"/>"
          + "<message from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
          + "<body>the first message</body>"
          + "</message>"
          + "</forwarded>"
          + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
      .body("this is my body !").build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds a complete XMPP message")
  public void buildCompleteMessage() {
    String hoped = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
      + "<message xmlns=\"jabber:client\" from=\"sender-id\" id=\"message-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<x xmlns=\"urn:xmpp:muclight:0#configuration\">"
      + "<operation>roomNameChanged</operation>"
      + "<config1>option one</config1>"
      + "<config2>option two</config2>"
      + "</x>"
      + "<body>this is my body !</body>"
      + "<reply xmlns=\"urn:xmpp:reply:0\" id=\"reply-id\" to=\"recipient-id\"/>"
      + "<forwarded xmlns=\"urn:xmpp:forward:0\">"
      + "<delay xmlns=\"urn:xmpp:delay\" stamp=\"2023-01-01T00:00:00Z\"/>"
      + "<message from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
      + "<body>this is the body of the message to forward!</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageId("message-id")
      .type(MessageType.ROOM_NAME_CHANGED)
      .addConfig("config1", "option one")
      .addConfig("config2", "option two")
      .body("this is my body !")
      .replyId("reply-id")
      .messageToForward("<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\" type=\"groupchat\">"
        + "<body>this is the body of the message to forward!</body>"
        + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
      .build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }
}