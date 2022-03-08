package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.it.annotations.IntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.HashDto;
import com.zextras.carbonio.chats.model.IdDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.InviteDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.RoomDetailsDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@IntegrationTest
public class RoomsApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final UserManagementMockServer  userManagementMockServer;
  private final MongooseImMockServer      mongooseImMockServer;
  private final StorageMockServer         storageMockServer;

  private final AppClock clock;

  public RoomsApiIT(
    RoomsApi roomsApi, ResteasyRequestDispatcher dispatcher, ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    UserManagementMockServer userManagementMockServer,
    MongooseImMockServer mongooseImMockServer, StorageMockServer storageMockServer, Clock clock
  ) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.userManagementMockServer = userManagementMockServer;
    this.mongooseImMockServer = mongooseImMockServer;
    this.storageMockServer = storageMockServer;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.clock = (AppClock) clock;
  }

  private static UUID   user1Id;
  private static UUID   user2Id;
  private static UUID   user3Id;
  private static UUID   user4Id;
  private static String user1Token;
  private static String user3Token;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccounts().get(0).getUUID();
    user1Token = MockedAccount.getAccounts().get(0).getToken();
    user2Id = MockedAccount.getAccounts().get(1).getUUID();
    user3Id = MockedAccount.getAccounts().get(2).getUUID();
    user3Token = MockedAccount.getAccounts().get(2).getToken();
    user4Id = MockedAccount.getAccounts().get(3).getUUID();
  }

  @Nested
  @DisplayName("Gets rooms list tests")
  public class GetsRoomsListTests {

    private static final String URL = "/rooms";

    @Test
    @DisplayName("Correctly gets the rooms list of authenticated user")
    public void listRoom_testOk() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(room2Id, RoomTypeDto.GROUP, "room2",
        List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(2, rooms.size());
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(room1Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(room2Id)));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly returns an empty list if the authenticated user isn't a member for any room")
    public void listRoom_testNoRooms() throws Exception {
      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(0, rooms.size());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("If there isn't an authenticated user return a status code 401")
    public void listRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(URL, null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Insert room tests")
  public class InsertRoomTests {

    private static final String URL = "/rooms";

    @Test
    @DisplayName("Given creation fields, inserts a new room and returns its data")
    public void insertRoom_testOk() throws Exception {
      Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

      clock.fixTimeAt(executionInstant);
      MockHttpResponse response;
      UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
        uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        response = dispatcher.post(URL,
          getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
          user1Token);
      }
      clock.removeFixTime();
      userManagementMockServer.verify("GET", String.format("/users/id/%s", user2Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/users/id/%s", user3Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      assertEquals(201, response.getStatus());
      RoomInfoDto room = objectMapper.readValue(response.getContentAsString(), RoomInfoDto.class);
      assertEquals("testRoom", room.getName());
      assertEquals("Test room", room.getDescription());
      assertEquals(RoomTypeDto.GROUP, room.getType());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
      assertTrue(
        room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().get().isOwner());
      assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
      assertEquals(executionInstant, room.getCreatedAt().toInstant());
      assertEquals(executionInstant, room.getUpdatedAt().toInstant());

      mongooseImMockServer.verify("PUT", "/admin/muc-lights/localhost",
        new RoomDetailsDto()
          .id(room.getId().toString())
          .owner(String.format("%s@localhost", user1Id))
          .name(room.getId().toString())
          .subject(room.getDescription()), 1);
      mongooseImMockServer.verify("POST",
        String.format("/admin/muc-lights/localhost/%s/participants", room.getId()),
        new InviteDto()
          .sender(String.format("%s@localhost", user1Id.toString()))
          .recipient(String.format("%s@localhost", user2Id.toString())), 1);
      mongooseImMockServer.verify("POST",
        String.format("/admin/muc-lights/localhost/%s/participants", room.getId()),
        new InviteDto()
          .sender(String.format("%s@localhost", user1Id.toString()))
          .recipient(String.format("%s@localhost", user3Id.toString())), 1);

      // TODO: 23/02/22 verify event dispatcher interactions

    }

    @Test
    @DisplayName("Given creation fields, if there isn't name field returns a status code 400")
    public void insertRoom_testErrorRequestWithoutName() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        getInsertRoomRequestBody(null, "Test room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
        user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given creation fields, if there aren't at least two member invitations returns a status code 400")
    public void insertRoom_testErrorRequestWithLessThanTwoMemberInvitations() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.GROUP, List.of(user2Id)),
        user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given creation fields, if there isn't an authenticated user returns a status code 401")
    public void insertRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        getInsertRoomRequestBody("room", "Room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
        null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given creation fields, if there are duplicated members returns a status code 400")
    public void insertRoom_testErrorDuplicatedMembers() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        getInsertRoomRequestBody("room", "Room", RoomTypeDto.GROUP, List.of(user2Id, user2Id, user3Id)),
        user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given creation fields, if there is current user into invites list returns a status code 400")
    public void insertRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        getInsertRoomRequestBody("room", "Room", RoomTypeDto.GROUP, List.of(user1Id, user2Id, user3Id)),
        user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given creation fields, if there is a unknown member returns a status code 404")
    public void insertRoom_testErrorUnknownMember() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.GROUP,
          List.of(user2Id, user3Id, UUID.randomUUID())),
        user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    private String getInsertRoomRequestBody(
      @Nullable String name, @Nullable String description, @Nullable RoomTypeDto type, @Nullable List<UUID> membersIds
    ) {
      StringBuilder stringBuilder = new StringBuilder();

      Optional.ofNullable(name).ifPresent(n -> stringBuilder.append(String.format("\"name\": \"%s\",", n)));
      Optional.ofNullable(description)
        .ifPresent(d -> stringBuilder.append(String.format("\"description\": \"%s\",", d)));
      Optional.ofNullable(type).ifPresent(t -> stringBuilder.append(String.format("\"type\": \"%s\",", t)));
      Optional.ofNullable(membersIds).ifPresent(ids -> {
        stringBuilder.append("\"membersIds\": [");
        stringBuilder.append(ids.stream().map(id -> String.format("\"%s\"", id)).collect(Collectors.joining(",")));
        stringBuilder.append("]");
      });
      if (',' == stringBuilder.charAt(stringBuilder.length() - 1)) {
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      }
      return String.format("{%s}", stringBuilder);
    }
  }

  @Nested
  @DisplayName("Gets room by identifier tests")
  public class GetsRoomByIdentifierTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the room information with members and user settings")
    public void getRoom_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "testRoom",
        List.of(user1Id, user2Id, user3Id), List.of(user1Id), List.of(user1Id));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(200, response.getStatus());
      RoomInfoDto room = objectMapper.readValue(response.getContentAsString(), RoomInfoDto.class);
      assertEquals(roomId, room.getId());
      assertEquals("testRoom", room.getName());
      assertNotNull(room.getMembers());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
      assertNotNull(room.getUserSettings());
      assertTrue(room.getUserSettings().isMuted());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not authenticated return a status code 401")
    public void getRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a fake room identifier returns a status code 404")
    public void getRoom_testErrorFakeRoomIdentifier() throws Exception {
      UUID roomId = UUID.randomUUID();

      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then return a status code 403")
    public void getRoom_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Updates room tests")
  public class UpdatesRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a room identifier and update fields, correctly updates the room")
    public void updateRoom_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
      Instant insertRoomInstant = executionInstant.minus(Duration.ofDays(1L)).truncatedTo(ChronoUnit.SECONDS);
      clock.fixTimeAt(insertRoomInstant);
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "testRoom",
        List.of(user1Id, user2Id, user3Id));
      clock.fixTimeAt(executionInstant);
      MockHttpResponse response = dispatcher.put(url(roomId),
        getUpdateRoomRequestBody("updatedRoom", "Updated room"), user1Token);
      clock.removeFixTime();
      assertEquals(200, response.getStatus());
      RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
      assertEquals("updatedRoom", room.getName());
      assertEquals("Updated room", room.getDescription());
      assertEquals(insertRoomInstant, room.getCreatedAt().toInstant());
      assertEquals(executionInstant, room.getUpdatedAt().toInstant());
      assertEquals(Duration.ofDays(1L), Duration.between(room.getCreatedAt(), room.getUpdatedAt()));

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and update fields, if there isn't the name return a status code 400")
    public void updateRoom_testErrorUpdateFieldWithoutName() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response = dispatcher.put(url(roomId),
        getUpdateRoomRequestBody(null, "Updated room"), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and update fields, if the user is not authenticated return a status code 401")
    public void updateRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        getUpdateRoomRequestBody("updatedRoom", "Updated room"), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a fake room identifier returns a status code 404")
    public void updateRoom_testErrorFakeRoomIdentifier() throws Exception {
      UUID roomId = UUID.randomUUID();

      MockHttpResponse response = dispatcher.put(url(roomId),
        getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and update fields, " +
      "if authenticated user isn't a room member then return a status code 403")
    public void updateRoom_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId),
        getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and update fields, " +
      "if authenticated user isn't a room owner then return a status code 403")
    public void updateRoom_testErrorUserIsNotARoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId),
        getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    private String getUpdateRoomRequestBody(@Nullable String name, @Nullable String description) {
      StringBuilder stringBuilder = new StringBuilder();
      Optional.ofNullable(name).ifPresent(n -> stringBuilder.append(String.format("\"name\": \"%s\",", n)));
      Optional.ofNullable(description)
        .ifPresent(d -> stringBuilder.append(String.format("\"description\": \"%s\"", d)));
      if (',' == stringBuilder.charAt(stringBuilder.length() - 1)) {
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      }
      return String.format("{%s}", stringBuilder);
    }
  }

  @Nested
  @DisplayName("Deletes room tests")
  public class DeletesRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly delete the room")
    public void deleteRoom_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "testRoom",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

      // TODO: 23/02/22 verify event dispatcher interactions
      mongooseImMockServer.verify("DELETE",
        String.format("/admin/muc-lights/localhost/%s/%s%%40localhost/management", roomId, user1Id), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not authenticated return status code 401")
    public void deleteRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room not exists returns a status code 404")
    public void deleteRoom_testErrorRoomNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, " +
      "if authenticated user isn't a room owner then return a status code 403")
    public void deleteRoom_testErrorUserIsNotARoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Gets room picture tests")
  public class GetsRoomPictureTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/picture", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the room picture")
    public void getRoomPicture_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(200, response.getStatus());

      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
        String.format("inline; filename=\"%s\"", fileMock.getName()),
        response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(fileMock.getMimeType(), response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("GET", "/download", fileMock.getId(), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not authenticated returns status code 401")
    public void getRoomPicture_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not a room member returns status code 403")
    public void getRoomPicture_testErrorAuthenticatedUserIsNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room hasn't the picture returns status code 404")
    public void getRoomPicture_testErrorRoomHasNoPicture() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the storage hasn't the picture file returns status code 500")
    public void getRoomPicture_testErrorStorageHasNoPictureFile() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(roomId, FileMetadataType.ROOM_AVATAR, user1Id, roomId);
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(500, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Update room picture tests")
  public class UpdateRoomPictureTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/picture", roomId);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, correctly updates the room pictures")
    public void updateRoomPicture_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getId().getBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        user1Token);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      // TODO: 01/03/22 verify event dispatcher iterations
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if user is not authenticated returns status code 401")
    public void updateRoomPicture_testErrorUnauthenticatedUser() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response = dispatcher.put(url(UUID.fromString(fileMock.getId())), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if X-Content-Disposition is missing returns status code 400")
    public void updateRoomPicture_testErrorMissingXContentDisposition() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response = dispatcher.put(url(UUID.fromString(fileMock.getId())), fileMock.getFileBytes(),
        Collections.singletonMap("Content-Type", "application/octet-stream"),
        user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if user is not a room member returns status code 403")
    public void updateRoomPicture_testErrorUserIsNotRoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if the room isn't a group returns status code 400")
    public void updateRoomPicture_testErrorRoomIsNotRoomGroup() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if the image is too large returns status code 400")
    public void updateRoomPicture_testErrorImageTooLarge() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_LARGE_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if the file isn't an image returns status code 400")
    public void updateRoomPicture_testErrorFileIsNotImage() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_PDF);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Mutes room for authenticated user tests")
  public class MutesRoomForAuthenticatedUserTests {

  }

  @Nested
  @DisplayName("Unmute room for authenticated user tests")
  public class UnmuteRoomForAuthenticatedUserTests {

  }

  @Nested
  @DisplayName("Reset room hash tests")
  public class ResetRoomHashTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/hash", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly reset room hash")
    public void resetRoomHash_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      clock.fixTimeAt(Instant.now().minus(Duration.ofDays(1)));
      String hash = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id)).getHash();
      clock.removeFixTime();
      MockHttpResponse response = dispatcher.put(url(roomId), null, user1Token);

      assertEquals(200, response.getStatus());
      assertNotEquals(hash, objectMapper.readValue(response.getContentAsString(), HashDto.class).getHash());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      // TODO: 23/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName("Given a room identifier, if the user isn't authenticated returns status code 401")
    public void resetRoomHash_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), null, null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exist returns status code 404")
    public void resetRoomHash_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), null, user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not a member of the room returns status code 403")
    public void resetRoomHash_testErrorUserIsNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Gets list of room members tests")
  public class GetsListOfRoomMembersTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/members", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the list of room members")
    public void listRoomMember_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(200, response.getStatus());
      List<MemberDto> members = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, members.size());
      assertTrue(members.stream().anyMatch(m -> user1Id.equals(m.getUserId())));
      assertTrue(members.stream().anyMatch(m -> user2Id.equals(m.getUserId())));
      assertTrue(members.stream().anyMatch(m -> user3Id.equals(m.getUserId())));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if user isn't authenticated returns status code 401")
    public void listRoomMember_testErrorUnauthenticatedUser() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if room doesn't exist returns status code 404")
    public void listRoomMember_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if user isn't a room member returns status code 403")
    public void listRoomMember_testErrorUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Inserts a room member tests")
  public class InsertsRoomMemberTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/members", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly insert the new room member")
    public void insertRoomMember_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id));

      MemberDto requestMember = new MemberDto();
      requestMember.setUserId(user4Id);
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(requestMember), user1Token);

      assertEquals(201, response.getStatus());
      MemberDto member = objectMapper.readValue(response.getContentAsString(), MemberDto.class);
      assertEquals(requestMember, member);

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      mongooseImMockServer.verify("POST",
        String.format("/admin/muc-lights/localhost/%s/participants", roomId),
        new InviteDto()
          .sender(String.format("%s@localhost", user1Id.toString()))
          .recipient(String.format("%s@localhost", user4Id.toString())), 1);

      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if there isn't an authenticated user returns status code 401")
    public void insertRoomMember_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.post(url(UUID.randomUUID()),
        getInsertRoomMemberRequestBody(user1Id), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exist returns status code 404")
    public void insertRoomMember_testRoomNotExist() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(user1Id), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the authenticated user isn't a room owner returns status code 403")
    public void insertRoomMember_testAuthenticateUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(user4Id), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room is one to one returns status code 400")
    public void insertRoomMember_testRoomOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, "room",
        List.of(user1Id, user2Id));
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(user4Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is already a room member returns status code 400")
    public void insertRoomMember_testUserAlreadyRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(user3Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user hasn't an account returns status code 404")
    public void insertRoomMember_testUserNotHasAccount() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }


    private String getInsertRoomMemberRequestBody(MemberDto member) {
      return getInsertRoomMemberRequestBody(
        member.getUserId(), member.isOwner(), member.isTemporary(), member.isExternal());
    }

    private String getInsertRoomMemberRequestBody(@Nullable UUID userId) {
      return getInsertRoomMemberRequestBody(userId, null, null, null);
    }

    private String getInsertRoomMemberRequestBody(
      @Nullable UUID userId, @Nullable Boolean owner, @Nullable Boolean temporary, @Nullable Boolean external
    ) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("{");
      Optional.ofNullable(userId).ifPresent(u -> stringBuilder.append(String.format("\"userId\": \"%s\",", u)));
      Optional.ofNullable(owner).ifPresent(o -> stringBuilder.append(String.format("\"owner\": %s,", o)));
      Optional.ofNullable(temporary).ifPresent(t -> stringBuilder.append(String.format("\"temporary\": %s,", t)));
      Optional.ofNullable(external).ifPresent(e -> stringBuilder.append(String.format("\"external\": %s", e)));
      if (',' == stringBuilder.charAt(stringBuilder.length() - 1)) {
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      }
      stringBuilder.append("}");
      return stringBuilder.toString();
    }
  }

  @Nested
  @DisplayName("Removes a member from the room tests")
  public class RemovesRoomMemberTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s", roomId, userId);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, correctly remove the user from room members")
    public void deleteRoomMember_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
        .isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify("DELETE", String.format("/api/rooms/%s/users/%s%%40localhost", roomId, user2Id), 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if there isn't an authenticated user returns status code 401")
    public void deleteRoomMember_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user1Id), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if room doesn't exist returns status code 404")
    public void deleteRoomMember_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user1Id), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the authenticated user isn't a room owner returns status code 403")
    public void deleteRoomMember_testErrorUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the room is ont to one returns status code 403")
    public void deleteRoomMember_testErrorRoomOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Promotes a member to owner tests")
  public class PromotesMemberToOwnerTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s/owner", roomId, userId);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, correctly promotes the member to owner")
    public void updateToOwner_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
        room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny().get()
          .isOwner());

      // TODO: 25/02/22 verify event dispatcher interactions
      // TODO: 25/02/22 verify mongoose
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if there isn't an authenticated user return status code 401")
    public void updateToOwner_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID(), user1Id), null, null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the room doesn't exist return status code 404")
    public void updateToOwner_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID(), user2Id), null, user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the authenticated user isn't a room owner return status code 403")
    public void updateToOwner_testAuthenticateUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the member doesn't exist return status code 403")
    public void updateToOwner_testErrorMemberNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user4Id), null, user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Demotes a member from owner to normal member tests")
  public class DemotesRoomMember_testOk {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s/owner", roomId, userId);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, correctly demotes a owner to normal member")
    public void deleteOwner_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id),
        List.of(user1Id, user2Id, user3Id), List.of());
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertFalse(
        room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny().get()
          .isOwner());

      // TODO: 28/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      // TODO: 28/02/22 verify mongooseIm interactions
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if there isn't an authenticated user returns status code 401")
    public void deleteOwner_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user2Id), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if authenticated user isn't a room owner returns status code 403")
    public void deleteOwner_testErrorAuthenticatedUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id),
        List.of(user2Id, user3Id), List.of());
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if requested user isn't a room member returns status code 403")
    public void deleteOwner_testErrorUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Gets list of room attachment information tests")
  public class ListOfRoomAttachmentInformationTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/attachments", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns all attachments info of the required room")
    public void listRoomAttachmentInfo_testOk() throws Exception {

      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.removeFixTime();

      MockHttpResponse response = dispatcher.get(url(room1Id), user1Token);

      assertEquals(200, response.getStatus());
      List<AttachmentDto> attachments = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, attachments.size());
      assertEquals(
        List.of(file3.getId(), file1.getId(), file2.getId()),
        attachments.stream()
          .map(attachment -> attachment.getId().toString())
          .collect(Collectors.toList()));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not authenticated return a status code 401")
    public void listRoomAttachmentInfo_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then return a status code 403")
    public void listRoomAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Insert attachment tests")
  public class InsertAttachmentTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/attachments", roomId);
    }

    @Test
    @DisplayName("Given a room identifier and an attachment, correctly inserts the attachment")
    public void insertAttachment_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        response = dispatcher.post(url(roomId), fileMock.getFileBytes(),
          Map.of("Content-Type", "application/octet-stream",
            "X-Content-Disposition",
            String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
          user1Token);
      }

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      assertEquals(201, response.getStatus());
      IdDto id = objectMapper.readValue(response.getContentAsString(), IdDto.class);

      assertTrue(
        integrationTestUtils.getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
          .stream().anyMatch(attach ->
            attach.getId().equals(id.getId().toString())));
      // TODO: 28/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName("Given a room identifier and an attachment, if there isn't an authenticated user returns a status code 401")
    public void insertAttachment_testErrorUnauthenticatedUser() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response = dispatcher.post(url(UUID.randomUUID()), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier and an attachment, if authenticated isn't a room member returns a status code 403")
    public void insertAttachment_testErrorAuthenticatedUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      MockHttpResponse response = dispatcher.post(url(roomId), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream",
          "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", fileMock.getName(), fileMock.getMimeType())),
        user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }
}