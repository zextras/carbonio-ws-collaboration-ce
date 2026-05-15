// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcherClient;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlMutation;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.StanzaResponse;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.XmppMessageBuilder;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.XmppMessageFactory;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.XmppMessageFactory.ForwardedAttachmentParams;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class MessageDispatcherMongooseImpl implements MessageDispatcher {

  private static final String DOMAIN = "carbonio";
  private static final String MUC_DOMAIN = "muclight.carbonio";

  private final MessageDispatcherClient messageDispatcherClient;

  @Inject
  public MessageDispatcherMongooseImpl(MessageDispatcherClient messageDispatcherClient) {
    this.messageDispatcherClient = messageDispatcherClient;
  }

  @Override
  public boolean isAlive() {
    try {
      messageDispatcherClient.executeQuery(GraphQlMutation.checkAuth());
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void createRoom(
      String roomId, String senderId, List<String> memberIds, boolean sendAffiliationMessages) {
    messageDispatcherClient.executeMutation(
        GraphQlMutation.createRoom(MUC_DOMAIN, roomId, userIdToUserDomain(senderId)));
    memberIds.forEach(
        member -> {
          addRoomMember(roomId, senderId, member);
          if (sendAffiliationMessages) {
            sendAffiliationMessage(roomId, senderId, member, MessageType.MEMBER_ADDED);
          }
        });
  }

  @Override
  public void updateRoomName(String roomId, String senderId, String name) {
    sendStanza(XmppMessageFactory.buildRoomNameChangedMessage(roomId, senderId, name));
  }

  @Override
  public void updateRoomDescription(String roomId, String senderId, String description) {
    sendStanza(
        XmppMessageFactory.buildRoomDescriptionChangedMessage(roomId, senderId, description));
  }

  @Override
  public void updateRoomPicture(
      String roomId, String senderId, String pictureId, String pictureName) {
    sendStanza(
        XmppMessageFactory.buildRoomPictureUpdatedMessage(
            roomId, senderId, pictureId, pictureName));
  }

  @Override
  public void deleteRoomPicture(String roomId, String senderId) {
    sendStanza(XmppMessageFactory.buildRoomPictureDeletedMessage(roomId, senderId));
  }

  @Override
  public void clearRoomHistory(String roomId, String senderId, String timestamp) {
    sendStanza(XmppMessageFactory.buildRoomHistoryClearedMessage(roomId, senderId, timestamp));
  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {
    messageDispatcherClient.executeMutation(
        GraphQlMutation.inviteUser(
            roomIdToRoomDomain(roomId),
            userIdToUserDomain(senderId),
            userIdToUserDomain(recipientId)));
  }

  @Override
  public void removeRoomMember(String roomId, String userIdToRemove) {
    messageDispatcherClient.executeMutation(
        GraphQlMutation.kickUser(roomIdToRoomDomain(roomId), userIdToUserDomain(userIdToRemove)));
  }

  @Override
  public void sendAffiliationMessage(
      String roomId, String senderId, String memberId, MessageType messageType) {
    sendStanza(
        XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
            .type(messageType)
            .addConfig("user-id", memberId)
            .build());
  }

  @Override
  public void addUsersToContacts(String user1id, String user2id) {
    messageDispatcherClient.executeMutation(
        GraphQlMutation.setMutualSubscription(
            userIdToUserDomain(user1id), userIdToUserDomain(user2id)));
  }

  @Override
  public StanzaResponse sendAttachment(
      String roomId,
      String senderId,
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area) {
    return sendStanza(
        XmppMessageFactory.buildAttachmentAddedMessage(
            roomId,
            senderId,
            new XmppMessageFactory.AttachmentMessageParams(
                fileId, fileName, mimeType, originalSize, description, messageId, replyId, area)));
  }

  @Override
  public Optional<String> getAttachmentIdFromMessage(String message) {
    return XmppMessageFactory.getAttachmentId(message);
  }

  @Override
  public void forwardMessage(
      String roomId,
      String senderId,
      ForwardMessageDto messageToForward,
      @Nullable FileMetadata fileMetadata) {
    ForwardedAttachmentParams attachment =
        fileMetadata == null
            ? null
            : new ForwardedAttachmentParams(
                fileMetadata.getId(),
                fileMetadata.getName(),
                fileMetadata.getMimeType(),
                fileMetadata.getOriginalSize(),
                fileMetadata.getMessageId(),
                fileMetadata.getStanzaId());
    sendStanza(
        XmppMessageFactory.buildForwardedMessage(
            roomId,
            senderId,
            messageToForward.getOriginalMessage(),
            messageToForward.getOriginalMessageSentAt(),
            messageToForward.getDescription(),
            attachment));
  }

  @Override
  public void sendMeetingStarted(String roomId, String senderId) {
    sendStanza(XmppMessageFactory.buildMeetingStartMessage(roomId, senderId));
  }

  @Override
  public void sendMeetingEnded(
      String roomId, String senderId, OffsetDateTime startedAt, long duration) {
    sendStanza(XmppMessageFactory.buildMeetingEndedMessage(roomId, senderId, startedAt, duration));
  }

  @Override
  public void sendMeetingDeclined(String roomId, String senderId) {
    sendStanza(XmppMessageFactory.buildMeetingDeclineMessage(roomId, senderId));
  }

  private StanzaResponse sendStanza(String message) {
    var payload =
        messageDispatcherClient
            .executeMutation(GraphQlMutation.sendStanza(message))
            .getData()
            .path("stanza")
            .path("sendStanza");
    return new StanzaResponse(
        payload.path("id").asText(null), payload.path("stanza_id").asText(null));
  }

  private String roomIdToRoomDomain(String roomId) {
    return String.join("@", roomId, MUC_DOMAIN);
  }

  private String userIdToUserDomain(String userId) {
    return String.join("@", userId, DOMAIN);
  }
}
