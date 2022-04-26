// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.mongooseim.admin.api.CommandsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.model.ChatMessageDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.InviteDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.RoomDetailsDto;
import com.zextras.carbonio.chats.mongooseim.client.api.RoomsApi;
import java.util.Base64;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageDispatcherImpl implements MessageDispatcher {

  private static final String XMPP_HOST = "carbonio";

  private final MucLightManagementApi mucLightManagementApi;
  private final CommandsApi           commandsApi;
  private final RoomsApi              roomsApi;

  @Inject
  public MessageDispatcherImpl(
    MucLightManagementApi mucLightManagementApi, CommandsApi commandsApi,
    RoomsApi roomsApi
  ) {
    this.mucLightManagementApi = mucLightManagementApi;
    this.commandsApi = commandsApi;
    this.roomsApi = roomsApi;
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
  public void deleteRoom(String roomId, String userIc) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameUserManagementDelete(XMPP_HOST, roomId,
      userId2userDomain(userIc));
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
