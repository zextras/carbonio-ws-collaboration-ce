package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.it.annotations.IntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class RoomsApiIT {

  private final RoomRepository            roomRepository;
  private final FileMetadataRepository    fileMetadataRepository;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;

  public RoomsApiIT(
    RoomsApi roomsApi, RoomRepository roomRepository, FileMetadataRepository fileMetadataRepository,
    ObjectMapper objectMapper, ResteasyRequestDispatcher dispatcher
  ) {
    this.roomRepository = roomRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.objectMapper = objectMapper;
    this.dispatcher = dispatcher;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @Test
  public void listRoom_test() throws Exception {
    UUID room1Id = UUID.randomUUID();
    UUID room2Id = UUID.randomUUID();
    UUID user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
    UUID user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
    UUID user3Id = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");

    Room room1 = generateRoom(room1Id, "room1", List.of(user1Id, user2Id, user3Id));
    Room room2 = generateRoom(room2Id, "room2", List.of(user1Id, user2Id));
    roomRepository.insert(room1);
    roomRepository.insert(room2);

    MockHttpResponse response = dispatcher.get("/rooms", user1Id.toString());
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
    });
    assertEquals(2, rooms.size());
  }

  @Test
  public void getRoomPicture_test() throws URISyntaxException {
    UUID roomId = UUID.randomUUID();
    UUID user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
    UUID user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
    UUID user3Id = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");

    Room room1 = generateRoom(roomId, "room1", List.of(user1Id, user2Id, user3Id));
    roomRepository.insert(room1);
    fileMetadataRepository.save(
      FileMetadata.create()
        .id(roomId.toString())
        .name("test-1.jpg")
        .originalSize(37863L)
        .mimeType("image/jpg")
        .type(FileMetadataType.ROOM_AVATAR)
        .userId(user1Id.toString())
        .roomId(roomId.toString()));
    MockHttpResponse response = dispatcher.get(String.format("/rooms/%s/picture", roomId), "332a9527-3388-4207-be77-6d7e2978a723");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  private Room generateRoom(UUID roomId, String name, List<UUID> usersIds) {
    Room room = Room.create();
    room
      .id(roomId.toString())
      .name(name)
      .type(RoomTypeDto.GROUP)
      .hash(Utils.encodeUuidHash(roomId.toString()))
      .subscriptions(usersIds.stream().map(userId ->
        Subscription.create()
          .id(new SubscriptionId(room.getId(), userId.toString()))
          .userId(userId.toString())
          .room(room)
          .joinedAt(OffsetDateTime.now())
      ).collect(Collectors.toList()));
    room.getSubscriptions().get(0).owner(true);
    return room;
  }
}