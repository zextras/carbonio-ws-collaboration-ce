package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.annotations.IntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class RoomsApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;

  public RoomsApiIT(
    RoomsApi roomsApi, ResteasyRequestDispatcher dispatcher, ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils
  ) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  private static UUID   user1Id;
  private static UUID   user2Id;
  private static UUID   user3Id;
  private static String user1Token;
  private static String user3Token;
  private static UUID   roomWithPicture;


  @BeforeAll
  public static void initAll() {
    int i = new Random().nextInt(Integer.MAX_VALUE);
    user1Id = MockedAccount.getAccount(i).getUUID();
    user1Token = MockedAccount.getAccount(i).getToken();
    user2Id = MockedAccount.getAccount(i + 1).getUUID();
    user3Id = MockedAccount.getAccount(i + 2).getUUID();
    user3Token = MockedAccount.getAccount(i + 2).getToken();
  }

  @Test
  public void listRoom_test() throws Exception {
    UUID room1Id = UUID.randomUUID();
    UUID room2Id = UUID.randomUUID();

    integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
    integrationTestUtils.generateAndSaveRoom(room2Id, RoomTypeDto.GROUP, "room2", List.of(user1Id, user2Id));

    MockHttpResponse response = dispatcher.get("/rooms", user1Token);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
    });
    assertEquals(2, rooms.size());
  }

  @Test
  @DisplayName("Given creation fields, if there is current user into invites list throws a 'bad request' exception")
  public void insertRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser() throws Exception {
    MockHttpResponse response = dispatcher.post("/rooms",
      getInsertRoomRequestBody("room", "Room", RoomTypeDto.GROUP, List.of(user1Id, user2Id, user3Id)),
      null, user1Token);

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
  }

  private String getInsertRoomRequestBody(
    String name, @Nullable String description, RoomTypeDto typeDto, @Nullable List<UUID> membersIds
  ) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append(String.format("\"name\": \"%s\",", name));
    Optional.ofNullable(description).ifPresent(desc -> sb.append(String.format("\"description\": \"%s\",", desc)));
    sb.append(String.format("\"type\": \"%s\",", typeDto));
    Optional.ofNullable(membersIds).ifPresent(ids -> {
      sb.append("[");
      ids.forEach(id -> sb.append(String.format("\"%s\",", id)));
      sb.append("]");
    });
    sb.append("}");
    return sb.toString();
  }

  @Test
  public void getRoomPicture_test() throws URISyntaxException {
    FileMock fileMock = MockedFiles.getRandomImage();
    UUID roomId = UUID.fromString(fileMock.getId());
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
    integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/rooms/%s/picture", roomId), user1Token);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }


}