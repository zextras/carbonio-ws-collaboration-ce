// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmppMessageFactory {

  private static final String DOMAIN = "carbonio";
  private static final String MUC_DOMAIN = "muclight.carbonio";
  private static final String TIMESTAMP = "timestamp";
  private static final String ATTACHMENT_ID = "attachment-id";

  private XmppMessageFactory() {
    // utility class
  }

  /**
   * Parses an XMPP message and returns the attachment id if the message carries an {@code
   * attachmentAdded} operation.
   *
   * @param message raw XMPP XML string
   * @return the attachment id wrapped in an {@link Optional}, or empty if not present
   */
  public static Optional<String> getAttachmentId(String message) {
    if (!message.contains("<operation>attachmentAdded</operation>")) {
      return Optional.empty();
    }
    try {
      Element node =
          XmlUtils.createSecureDocumentBuilderFactory()
              .newDocumentBuilder()
              .parse(new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)))
              .getDocumentElement();
      Element x = (Element) node.getElementsByTagName("x").item(0);
      if (x != null) {
        Element op = (Element) x.getElementsByTagName("operation").item(0);
        if (op != null
            && MessageType.ATTACHMENT_ADDED.getName().equals(op.getFirstChild().getNodeValue())) {
          return Optional.ofNullable(x.getElementsByTagName(ATTACHMENT_ID).item(0))
              .map(Node::getFirstChild)
              .map(Node::getNodeValue);
        }
      }
    } catch (Exception e) {
      throw new MessageDispatcherException("Something went wrong while parsing the message: ", e);
    }
    return Optional.empty();
  }

  public record AttachmentMessageParams(
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area) {}

  public record ForwardedAttachmentParams(
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      @Nullable String messageId,
      @Nullable String stanzaId) {}

  /**
   * Builds an XMPP groupchat stanza to notify room members that a user declined a meeting.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>meetingDeclined</operation>
   *     <timestamp>1776782753548</timestamp>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildMeetingDeclineMessage(String roomId, String senderId) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.MEETING_DECLINED)
        .addConfig(TIMESTAMP, String.valueOf(System.currentTimeMillis()))
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that a meeting has started.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>meetingStarted</operation>
   *     <timestamp>1776782753548</timestamp>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildMeetingStartMessage(String roomId, String senderId) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.MEETING_STARTED)
        .addConfig(TIMESTAMP, String.valueOf(System.currentTimeMillis()))
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that a meeting has ended.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>meetingEnded</operation>
   *      <timestamp>1776782753548</timestamp>
   *      <startedAt>1776782753548</startedAt>
   *      <duration>15</duration>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildMeetingEndedMessage(
      String roomId, String senderId, OffsetDateTime startedAt, long duration) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.MEETING_ENDED)
        .addConfig(TIMESTAMP, String.valueOf(System.currentTimeMillis()))
        .addConfig("startedAt", String.valueOf(startedAt.toInstant().toEpochMilli()))
        .addConfig("duration", String.valueOf(duration))
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that the room name has changed.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>roomNameChanged</operation>
   *     <value encoded='UTF-8'>New Room Name</value>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildRoomNameChangedMessage(String roomId, String senderId, String name) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.ROOM_NAME_CHANGED)
        .addConfig("value", name, true)
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that the room description has changed.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>roomDescriptionChanged</operation>
   *     <value encoded='UTF-8'>New description</value>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildRoomDescriptionChangedMessage(
      String roomId, String senderId, String description) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.ROOM_DESCRIPTION_CHANGED)
        .addConfig("value", description, true)
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that the room picture has been updated.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>roomPictureUpdated</operation>
   *     <picture-id>abc-123</picture-id>
   *     <picture-name encoded='UTF-8'>avatar.png</picture-name>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildRoomPictureUpdatedMessage(
      String roomId, String senderId, String pictureId, String pictureName) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.ROOM_PICTURE_UPDATED)
        .addConfig("picture-id", pictureId)
        .addConfig("picture-name", pictureName, true)
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that the room picture has been deleted.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>roomPictureDeleted</operation>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildRoomPictureDeletedMessage(String roomId, String senderId) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.ROOM_PICTURE_DELETED)
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that the room history has been cleared.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>roomHistoryCleared</operation>
   *     <user-id>user-uuid</user-id>
   *     <cleared-at>2024-01-15T10:30:00Z</cleared-at>
   *   </x>
   *   <body/>
   * </message>
   * }</pre>
   */
  public static String buildRoomHistoryClearedMessage(
      String roomId, String senderId, String timestamp) {
    return XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
        .type(MessageType.ROOM_HISTORY_CLEARED)
        .addConfig("user-id", senderId)
        .addConfig("cleared-at", timestamp)
        .build();
  }

  /**
   * Builds an XMPP groupchat stanza to notify room members that an attachment has been added.
   * {@code messageId}, {@code replyId}, and {@code area} are optional and omitted when null.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'
   *          id='<messageId>'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>attachmentAdded</operation>
   *     <attachment-id>file-uuid</attachment-id>
   *     <filename encoded='UTF-8'>document.pdf</filename>
   *     <mime-type>application/pdf</mime-type>
   *     <size>204800</size>
   *     <area>meeting-area</area>
   *   </x>
   *   <body encoded='UTF-8'>Optional description</body>
   *   <reply xmlns='urn:xmpp:reply:0' to='<roomId>@muclight.carbonio' id=
   * '<replyId>'/>
   * </message>
   * }</pre>
   */
  public static String buildAttachmentAddedMessage(
      String roomId, String senderId, AttachmentMessageParams params) {
    XmppMessageBuilder builder =
        XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
            .type(MessageType.ATTACHMENT_ADDED)
            .addConfig("attachment-id", params.fileId())
            .addConfig("filename", params.fileName(), true)
            .addConfig("mime-type", params.mimeType())
            .addConfig("size", String.valueOf(params.originalSize()))
            .body(params.description())
            .messageId(params.messageId())
            .replyId(params.replyId());
    if (params.area() != null) {
      builder.addConfig("area", params.area());
    }
    return builder.build();
  }

  /**
   * Builds an XMPP groupchat stanza to forward an existing message to a room. The {@code
   * attachment} parameter is optional: when {@code null} the stanza forwards a plain text message;
   * when present, the stanza is also flagged as {@link MessageType#ATTACHMENT_ADDED} and carries
   * the attachment metadata.
   *
   * <p>Example output (text forward):
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'>
   *   <body encoded='UTF-8'>Optional description</body>
   *   <forwarded xmlns='urn:xmpp:forward:0'>
   *     <delay xmlns='urn:xmpp:delay' stamp='2024-01-15T10:30:00Z'/>
   *     <message>...original message...</message>
   *   </forwarded>
   * </message>
   * }</pre>
   *
   * <p>Example output (attachment forward):
   *
   * <pre>{@code
   * <message xmlns='jabber:client'
   *          to='<roomId>@muclight.carbonio'
   *          from='<senderId>@carbonio'
   *          type='groupchat'
   *          id='<messageId>'>
   *   <x xmlns='urn:xmpp:muclight:0#configuration'>
   *     <operation>attachmentAdded</operation>
   *     <attachment-id>file-uuid</attachment-id>
   *     <filename encoded='UTF-8'>document.pdf</filename>
   *     <mime-type>application/pdf</mime-type>
   *     <size>204800</size>
   *     <stanza-id>...</stanza-id>
   *   </x>
   *   <body encoded='UTF-8'>Optional description</body>
   *   <forwarded xmlns='urn:xmpp:forward:0'>...</forwarded>
   * </message>
   * }</pre>
   */
  public static String buildForwardedMessage(
      String roomId,
      String senderId,
      String originalMessage,
      OffsetDateTime originalMessageSentAt,
      @Nullable String description,
      @Nullable ForwardedAttachmentParams attachment) {
    XmppMessageBuilder builder =
        XmppMessageBuilder.create(toRoomJid(roomId), toUserJid(senderId))
            .messageToForward(originalMessage)
            .messageToForwardSentAt(originalMessageSentAt)
            .body(description);
    if (attachment != null) {
      builder
          .type(MessageType.ATTACHMENT_ADDED)
          .addConfig("attachment-id", attachment.fileId())
          .addConfig("filename", attachment.fileName(), true)
          .addConfig("mime-type", attachment.mimeType())
          .addConfig("size", String.valueOf(attachment.originalSize()))
          .messageId(attachment.messageId())
          .stanzaId(attachment.stanzaId());
    }
    return builder.build();
  }

  static String toRoomJid(String roomId) {
    return roomId + "@" + MUC_DOMAIN;
  }

  static String toUserJid(String userId) {
    return userId + "@" + DOMAIN;
  }
}
