// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

public class XmppMessageFactory {

  private static final String DOMAIN = "carbonio";
  private static final String MUC_DOMAIN = "muclight.carbonio";

  public record AttachmentMessageParams(
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area) {}

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
        .addConfig("timestamp", String.valueOf(System.currentTimeMillis()))
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
        .addConfig("timestamp", String.valueOf(System.currentTimeMillis()))
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
        .addConfig("timestamp", String.valueOf(System.currentTimeMillis()))
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

  static String toRoomJid(String roomId) {
    return roomId + "@" + MUC_DOMAIN;
  }

  static String toUserJid(String userId) {
    return userId + "@" + DOMAIN;
  }
}
