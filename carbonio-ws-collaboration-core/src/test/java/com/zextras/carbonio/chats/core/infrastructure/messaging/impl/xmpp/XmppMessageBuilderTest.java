// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import java.time.OffsetDateTime;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class XmppMessageBuilderTest {

  @Test
  @DisplayName("Builds an XMPP message with body")
  public void buildMessageWithBody() {
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
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
    String hoped = "<message xmlns='jabber:client' from='sender-id' id='message-id' to='recipient-id' type='groupchat'>"
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
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
      + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
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
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
      + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
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
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body>this is my body !</body>"
      + "<reply xmlns='urn:xmpp:reply:0' id='reply-id' to='recipient-id'/>"
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
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body/>"
      + "<forwarded xmlns='urn:xmpp:forward:0' count='1'>"
      + "<delay xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/>"
      + "<message from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body>\\\\u0074\\\\u006f\\\\u0020\\\\u0066\\\\u006f\\\\u0072\\\\u0077\\\\u0061\\\\u0072\\\\u0064</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageToForward("<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
        + "<body>to forward</body>"
        + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z")).build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message that forwards a replied message")
  public void buildMessageWithForwardOfRepliedMessage() {
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body/>"
      + "<forwarded xmlns='urn:xmpp:forward:0' count='1'>"
      + "<delay xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/>"
      + "<message from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body>\\\\u0074\\\\u006f\\\\u0020\\\\u0066\\\\u006f\\\\u0072\\\\u0077\\\\u0061\\\\u0072\\\\u0064</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageToForward("<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
        + "<body>to forward</body>"
        + "<reply xmlns='urn:xmpp:reply:0' id='reply-id' to='sender-id'/>"
        + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z")).build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds an XMPP message that forwards a forwarded message")
  public void buildMessageWithForwardOfForwardedMessage() {
    String hoped = "<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body/>"
      + "<forwarded xmlns='urn:xmpp:forward:0' count='2'>"
      + "<delay xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/>"
      + "<message from='old-sender-id' to='old-recipient-id' type='groupchat'>"
      + "<body>\\\\u0066\\\\u0069\\\\u0072\\\\u0073\\\\u0074\\\\u0020\\\\u0066\\\\u006f\\\\u0072\\\\u0077\\\\u0061\\\\u0072\\\\u0064</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageToForward(
        "<message xmlns='jabber:client' from='old-sender-id' to='old-recipient-id' type='groupchat'>"
          + "<body/>"
          + "<forwarded xmlns='urn:xmpp:forward:0' count='1'>"
          + "<delay xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/>"
          + "<message from='sender-id' to='recipient-id' type='groupchat'>"
          + "<body>first forward</body>"
          + "</message>"
          + "</forwarded>"
          + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z")).build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Test
  @DisplayName("Builds a complete XMPP message")
  public void buildCompleteMessage() {
    String hoped = "<message xmlns='jabber:client' from='sender-id' id='message-id' to='recipient-id' type='groupchat'>"
      + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
      + "<operation>roomNameChanged</operation>"
      + "<config1>option one</config1>"
      + "<config2>option two</config2>"
      + "</x>"
      + "<body/>"
      + "<reply xmlns='urn:xmpp:reply:0' id='reply-id' to='recipient-id'/>"
      + "<forwarded xmlns='urn:xmpp:forward:0' count='1'>"
      + "<delay xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/>"
      + "<message from='sender-id' to='recipient-id' type='groupchat'>"
      + "<body>\\\\u0074\\\\u006f\\\\u0020\\\\u0066\\\\u006f\\\\u0072\\\\u0077\\\\u0061\\\\u0072\\\\u0064</body>"
      + "</message>"
      + "</forwarded>"
      + "</message>";
    String result = XmppMessageBuilder.create("recipient-id", "sender-id")
      .messageId("message-id")
      .type(MessageType.ROOM_NAME_CHANGED)
      .addConfig("config1", "option one")
      .addConfig("config2", "option two")
      .replyId("reply-id")
      .messageToForward("<message xmlns='jabber:client' from='sender-id' to='recipient-id' type='groupchat'>"
        + "<body>to forward</body>"
        + "</message>")
      .messageToForwardSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
      .build();
    assertNotNull(result);
    assertEquals(hoped, result);
  }

  @Nested
  public class EncodeStringForXmppTest {

    @Test
    @DisplayName("Encodes a string with basic latin chars")
    public void encodeBasicLatinCharsForXmppTest() {
      String toEncode = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
      String hoped = "\\\\u0021\\\\u0022\\\\u0023\\\\u0024\\\\u0025\\\\u0026\\\\u0027\\\\u0028\\\\u0029\\\\u002a\\\\u002b\\\\u002c\\\\u002d\\\\u002e\\\\u002f\\\\u0030\\\\u0031\\\\u0032\\\\u0033\\\\u0034\\\\u0035\\\\u0036\\\\u0037\\\\u0038\\\\u0039\\\\u003a\\\\u003b\\\\u003c\\\\u003d\\\\u003e\\\\u003f\\\\u0040\\\\u0041\\\\u0042\\\\u0043\\\\u0044\\\\u0045\\\\u0046\\\\u0047\\\\u0048\\\\u0049\\\\u004a\\\\u004b\\\\u004c\\\\u004d\\\\u004e\\\\u004f\\\\u0050\\\\u0051\\\\u0052\\\\u0053\\\\u0054\\\\u0055\\\\u0056\\\\u0057\\\\u0058\\\\u0059\\\\u005a\\\\u005b\\\\u005c\\\\u005d\\\\u005e\\\\u005f\\\\u0060\\\\u0061\\\\u0062\\\\u0063\\\\u0064\\\\u0065\\\\u0066\\\\u0067\\\\u0068\\\\u0069\\\\u006a\\\\u006b\\\\u006c\\\\u006d\\\\u006e\\\\u006f\\\\u0070\\\\u0071\\\\u0072\\\\u0073\\\\u0074\\\\u0075\\\\u0076\\\\u0077\\\\u0078\\\\u0079\\\\u007a\\\\u007b\\\\u007c\\\\u007d\\\\u007e";
      assertEquals(hoped, XmppMessageBuilder.encodeStringForXmpp(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with supplement latin chars")
    public void encodeSupplementLatinCharsForXmppTest() {
      String toEncode = "¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
      String hoped = "\\\\u00a1\\\\u00a2\\\\u00a3\\\\u00a4\\\\u00a5\\\\u00a6\\\\u00a7\\\\u00a8\\\\u00a9\\\\u00aa\\\\u00ab\\\\u00ac\\\\u00ae\\\\u00af\\\\u00b0\\\\u00b1\\\\u00b2\\\\u00b3\\\\u00b4\\\\u00b5\\\\u00b6\\\\u00b7\\\\u00b8\\\\u00b9\\\\u00ba\\\\u00bb\\\\u00bc\\\\u00bd\\\\u00be\\\\u00bf\\\\u00c0\\\\u00c1\\\\u00c2\\\\u00c3\\\\u00c4\\\\u00c5\\\\u00c6\\\\u00c7\\\\u00c8\\\\u00c9\\\\u00ca\\\\u00cb\\\\u00cc\\\\u00cd\\\\u00ce\\\\u00cf\\\\u00d0\\\\u00d1\\\\u00d2\\\\u00d3\\\\u00d4\\\\u00d5\\\\u00d6\\\\u00d7\\\\u00d8\\\\u00d9\\\\u00da\\\\u00db\\\\u00dc\\\\u00dd\\\\u00de\\\\u00df\\\\u00e0\\\\u00e1\\\\u00e2\\\\u00e3\\\\u00e4\\\\u00e5\\\\u00e6\\\\u00e7\\\\u00e8\\\\u00e9\\\\u00ea\\\\u00eb\\\\u00ec\\\\u00ed\\\\u00ee\\\\u00ef\\\\u00f0\\\\u00f1\\\\u00f2\\\\u00f3\\\\u00f4\\\\u00f5\\\\u00f6\\\\u00f7\\\\u00f8\\\\u00f9\\\\u00fa\\\\u00fb\\\\u00fc\\\\u00fd\\\\u00fe\\\\u00ff";
      assertEquals(hoped, XmppMessageBuilder.encodeStringForXmpp(toEncode));
    }

    @Test
    @DisplayName("Encodes a string with  chars")
    public void encodeCharsForXmppTest() {
      String toEncode = "";
      String hoped = "";
      assertEquals(hoped, XmppMessageBuilder.encodeStringForXmpp(toEncode));
    }
  }

  @Test
  public void isEncodedForXmppTestSuccess() {
    assertTrue(XmppMessageBuilder.isEncodedForXmpp(
      "\\\\u0074\\\\u006f\\\\u0020\\\\u0066\\\\u006f\\\\u0072\\\\u0077\\\\u0061\\\\u0072\\\\u0064"));
  }
}