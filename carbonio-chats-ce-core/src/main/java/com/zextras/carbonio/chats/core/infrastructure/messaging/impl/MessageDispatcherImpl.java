// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.mongooseim.admin.api.CommandsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.ContactsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.OneToOneMessagesApi;
import com.zextras.carbonio.chats.mongooseim.admin.model.AddcontactDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.ChatMessageDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.InviteDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.Message1Dto;
import com.zextras.carbonio.chats.mongooseim.admin.model.RoomDetailsDto;
import com.zextras.carbonio.chats.mongooseim.client.api.RoomsApi;
import java.util.Base64;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.StandardExtensionElement.Builder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jxmpp.stringprep.XmppStringprepException;

@Singleton
public class MessageDispatcherImpl implements MessageDispatcher {

  private static final String XMPP_HOST      = "carbonio";
  private static final String ROOM_XMPP_HOST = "muclight.carbonio";

  private final MucLightManagementApi mucLightManagementApi;
  private final CommandsApi           commandsApi;
  private final RoomsApi              roomsApi;
  private final ContactsApi           contactsApi;
  private final OneToOneMessagesApi   oneToOneMessagesApi;

  @Inject
  public MessageDispatcherImpl(
    MucLightManagementApi mucLightManagementApi,
    CommandsApi commandsApi,
    RoomsApi roomsApi,
    ContactsApi contactsApi,
    OneToOneMessagesApi oneToOneMessagesApi
  ) {
    this.mucLightManagementApi = mucLightManagementApi;
    this.commandsApi = commandsApi;
    this.roomsApi = roomsApi;
    this.contactsApi = contactsApi;
    this.oneToOneMessagesApi = oneToOneMessagesApi;
  }

  @Override
  public void createRoom(Room room, String senderId) {
    try {
      mucLightManagementApi.mucLightsXMPPHostPut(XMPP_HOST, new RoomDetailsDto()
        .id(room.getId())
        .owner(userId2userDomain(senderId))
        .name(room.getId())
        .subject(room.getDescription()));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred when adding a room to MongooseIm", e);
    }
    room.getSubscriptions().stream()
      .filter(member -> !member.getUserId().equals(senderId))
      .forEach(member -> addRoomMember(room.getId(), senderId, member.getUserId()));
  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {
    try {
      mucLightManagementApi.mucLightsXMPPMUCHostRoomNameParticipantsPost(XMPP_HOST, roomId,
        new InviteDto()
          .sender(userId2userDomain(senderId))
          .recipient(userId2userDomain(recipientId))
      );
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred when adding a member to a MongooseIm room", e);
    }
  }

  @Override
  public void removeRoomMember(String roomId, String senderId, String recipientId) {
    // TODO: 22/12/21 add request to MongooseIM administrative tools
    RoomsApi roomsApi = getRoomsApi(senderId);
    roomsApi.roomsIdUsersUserDelete(roomId, userId2userDomain(recipientId));
  }

  @Override
  public void addUsersToContacts(String user1id, String user2id) {
    if (user1id.equals(user2id)) {
      return;
    }
    String user1jid = userId2userDomain(user1id);
    String user2jid = userId2userDomain(user2id);
    contactsApi.contactsUserPost(user1jid, new AddcontactDto().jid(user2jid));
    contactsApi.contactsUserPost(user2jid, new AddcontactDto().jid(user1jid));
  }

  @Override
  public void setMemberRole(String roomId, String senderId, String recipientId, boolean isOwner) {
    // TODO: 22/12/21 method non-existent in the MongooseIM
  }

  @Override
  public void sendMessageToRoom(String roomId, String senderId, String message) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameMessagesPost(
      XMPP_HOST, roomId, new ChatMessageDto()
        .from(userId2userDomain(senderId))
        .body(message)
    );
  }

  @Override
  public boolean isAlive() {
    try {
      commandsApi.commandsGet();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void deleteRoom(String roomId, String userId) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameUserManagementDelete(XMPP_HOST, roomId,
      userId2userDomain(userId));
  }

  private StandardExtensionElement getStanzasElementX(MessageType type, Map<String, String> elementsMap) {
    Builder x = StandardExtensionElement.builder("x", "urn:xmpp:muclight:0#configuration")
      .addElement("operation", type.getName());
    elementsMap.keySet().forEach(k -> x.addElement(k, elementsMap.get(k)));
    return x.build();
  }

  private String getStanzaMessage(String roomId, String senderId, MessageType type, Map<String, String> elementsMap)
    throws XmppStringprepException {
    return StanzaBuilder
      .buildMessage()
      .from(userId2userDomain(senderId))
      .to(roomId2roomDomain(roomId))
      .ofType(Type.groupchat)
      .addExtension(getStanzasElementX(type, elementsMap))
      .build()
      .toXML()
      .toString();
  }

  @Override
  public void updateRoomName(String roomId, String senderId, String name) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.CHANGED_ROOM_NAME, Map.of("value", name))));
    } catch (XmppStringprepException e) {
      throw new InternalErrorException(
        "An error occurred when sending a message for room name changed to a MongooseIm room", e);
    }
  }

  @Override
  public void updateRoomDescription(String roomId, String senderId, String description) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.CHANGED_ROOM_DESCRIPTION, Map.of("value", description))));
    } catch (XmppStringprepException e) {
      throw new InternalErrorException(
        "An error occurred when sending a message for room description changed to a MongooseIm room", e);
    }
  }

  @Override
  public void updateRoomPictures(String roomId, String senderId, String pictureId, String pictureName) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.UPDATED_ROOM_PICTURES,
          Map.of("picture-id", pictureId, "picture-name", pictureName))));
    } catch (XmppStringprepException e) {
      throw new InternalErrorException(
        "An error occurred when sending a message for room picture updated to a MongooseIm room", e);
    }
  }

  private String roomId2roomDomain(String roomId) {
    return String.join("@", roomId, ROOM_XMPP_HOST);
  }

  private String userId2userDomain(String userId) {
    return String.join("@", userId, XMPP_HOST);
  }

  private RoomsApi getRoomsApi(String userId) {
    // TODO: 22/12/21 set authorizations by cookies
    roomsApi.getApiClient().addDefaultHeader("Authorization",
      String.format("Basic %s",
        Base64.getEncoder().encodeToString(
          String.join(":", userId2userDomain(userId), "password").getBytes())));
    return roomsApi;
  }
}
