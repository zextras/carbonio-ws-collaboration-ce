package com.zextras.chats.core.infrastructure.messaging.impl;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.infrastructure.messaging.MessageService;
import com.zextras.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.chats.mongooseim.admin.model.ChatMessageDto;
import com.zextras.chats.mongooseim.admin.model.InviteDto;
import com.zextras.chats.mongooseim.admin.model.RoomDetailsDto;
import com.zextras.chats.mongooseim.client.api.RoomsApi;
import java.util.Base64;
import javax.inject.Inject;

public class MessageServiceImpl implements MessageService {

  private static final String XMPP_HOST = "localhost";

  private final MucLightManagementApi mucLightManagementApi;

  @Inject
  public MessageServiceImpl(MucLightManagementApi mucLightManagementApi) {
    this.mucLightManagementApi = mucLightManagementApi;
  }

  @Override
  public void createRoom(Room room, String senderId) {
    mucLightManagementApi.mucLightsXMPPHostPut(XMPP_HOST, new RoomDetailsDto()
      .id(room.getId())
      .owner(userId2userDomain(senderId))
      .name(room.getId())
      .subject(room.getDescription()));
    room.getSubscriptions().stream()
      .filter(member -> !member.getUserId().equals(senderId))
      .forEach(member -> addRoomMember(room.getId(), senderId, member.getUserId()));
  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameParticipantsPost(XMPP_HOST, roomId,
      new InviteDto()
        .sender(userId2userDomain(senderId))
        .recipient(userId2userDomain(recipientId))
    );
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
  public void attachmentAdded(String roomId, String senderId, String fileId) {
    // TODO: 04/01/22
  }

  @Override
  public void attachmentRemoved(String roomId, String senderId, String fileId) {
    // TODO: 04/01/22
  }

  @Override
  public void deleteRoom(String roomId, String userIc) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameUserManagementDelete(XMPP_HOST, roomId, userId2userDomain(userIc));
  }

  private String userId2userDomain(String userId) {
    return String.join("@", userId, XMPP_HOST);
  }

  private RoomsApi getRoomsApi(String userId) {
    // TODO: 22/12/21 set authorizations by cookies
    RoomsApi roomsApi = new RoomsApi();
    roomsApi.getApiClient().addDefaultHeader("Authorization",
      String.format("Basic %s",
        Base64.getEncoder().encodeToString(
          String.join(":", userId2userDomain(userId), "password").getBytes())));
    return roomsApi;
  }
}
