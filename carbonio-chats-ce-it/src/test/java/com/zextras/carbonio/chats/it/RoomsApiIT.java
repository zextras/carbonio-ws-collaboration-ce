// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils.RoomMemberField;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.ClearedDateDto;
import com.zextras.carbonio.chats.model.HashDto;
import com.zextras.carbonio.chats.model.IdDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomRankDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.ParticipantDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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
import org.mockserver.verify.VerificationTimes;

@ApiIntegrationTest
public class RoomsApiIT {

  private final ResteasyRequestDispatcher  dispatcher;
  private final ObjectMapper               objectMapper;
  private final IntegrationTestUtils       integrationTestUtils;
  private final MeetingTestUtils           meetingTestUtils;
  private final UserManagementMockServer   userManagementMockServer;
  private final MongooseImMockServer       mongooseImMockServer;
  private final StorageMockServer          storageMockServer;
  private final RoomRepository             roomRepository;
  private final FileMetadataRepository     fileMetadataRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;

  private final AppClock clock;

  public RoomsApiIT(
    RoomsApi roomsApi, ResteasyRequestDispatcher dispatcher, ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    MeetingTestUtils meetingTestUtils,
    UserManagementMockServer userManagementMockServer,
    MongooseImMockServer mongooseImMockServer, StorageMockServer storageMockServer,
    FileMetadataRepository fileMetadataRepository, Clock clock,
    RoomRepository roomRepository,
    RoomUserSettingsRepository roomUserSettingsRepository
  ) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.meetingTestUtils = meetingTestUtils;
    this.userManagementMockServer = userManagementMockServer;
    this.mongooseImMockServer = mongooseImMockServer;
    this.storageMockServer = storageMockServer;
    this.fileMetadataRepository = fileMetadataRepository;
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
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
    user1Id = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    user1Token = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    user2Id = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    user3Id = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
    user3Token = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getToken();
    user4Id = MockedAccount.getAccount(MockedAccountType.PEPERITA_PATTY).getUUID();
  }

  @Nested
  @DisplayName("Gets rooms list tests")
  public class GetsRoomsListTests {

    private String url(@Nullable Map<String, List<String>> queryParams) {
      StringBuilder url = new StringBuilder();
      Optional.ofNullable(queryParams).ifPresent(params ->
        params.forEach((key, values) ->
          values.forEach(v -> {
            url.append(url.length() > 0 ? "&" : "");
            url.append(String.join("=", key, v));
          })));
      return "/rooms" + (url.length() > 0 ? "?" + url : "");
    }

    @Test
    @DisplayName("Correctly gets the basic rooms of authenticated user")
    public void listRoom_testOkBasicRooms() throws Exception {
      UUID group1Id = UUID.randomUUID();
      UUID group2Id = UUID.randomUUID();
      UUID workspace1Id = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(group1Id, RoomTypeDto.GROUP, "room1",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(group2Id, RoomTypeDto.GROUP, "room2",
        List.of(user1Id, user2Id), List.of(user1Id), List.of(user1Id), OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveRoom(workspace1Id, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel1")
        .description("Channel one")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(2), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel2")
        .description("Channel two")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(1), List.of());

      MockHttpResponse response = dispatcher.get(url(null), user1Token);
      assertEquals(200, response.getStatus());
      assertFalse(response.getContentAsString().contains("members"));
      assertFalse(response.getContentAsString().contains("userSettings"));
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, rooms.size());
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(group1Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(group2Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(workspace1Id)));
      assertEquals(0, rooms.get(0).getMembers().size());
      assertEquals(0, rooms.get(1).getMembers().size());
      assertEquals(0, rooms.get(2).getMembers().size());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());
      assertNull(rooms.get(2).getUserSettings());
      assertTrue(rooms.stream().anyMatch(room -> room.getPictureUpdatedAt() != null && room.getPictureUpdatedAt()
        .equals(OffsetDateTime.parse("2022-01-01T00:00:00Z"))));
      assertTrue(rooms.stream().anyMatch(room -> room.getPictureUpdatedAt() == null));
      RoomDto workspace = rooms.stream().filter(room -> RoomTypeDto.WORKSPACE.equals(room.getType())).findAny()
        .orElseThrow();
      assertEquals(5, workspace.getRank());
      assertEquals(2, workspace.getChildren().size());
      assertTrue(workspace.getChildren().stream().anyMatch(child -> RoomTypeDto.CHANNEL.equals(child.getType())));
      assertTrue(workspace.getChildren().stream().anyMatch(child -> child.getMembers().isEmpty()));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly gets the rooms list with members of authenticated user")
    public void listRoom_testOkWithMembers() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      UUID workspace1Id = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(room2Id, RoomTypeDto.GROUP, "room2",
        List.of(user1Id, user2Id), List.of(user1Id), List.of(user1Id), OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveRoom(workspace1Id, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel1")
        .description("Channel one")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(2), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel2")
        .description("Channel two")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(1), List.of());

      MockHttpResponse response = dispatcher.get(url(Map.of("extraFields", List.of("members"))), user1Token);
      assertEquals(200, response.getStatus());
      assertFalse(response.getContentAsString().contains("userSettings"));
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, rooms.size());
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(room1Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(room2Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(workspace1Id)));
      assertNotNull(rooms.get(0).getMembers());
      assertNotNull(rooms.get(1).getMembers());
      assertNotNull(rooms.get(2).getMembers());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());
      assertNull(rooms.get(2).getUserSettings());
      assertTrue(rooms.stream().anyMatch(room -> room.getPictureUpdatedAt() != null && room.getPictureUpdatedAt()
        .equals(OffsetDateTime.parse("2022-01-01T00:00:00Z"))));
      assertTrue(rooms.stream().anyMatch(room -> room.getPictureUpdatedAt() == null));
      RoomDto workspace = rooms.stream().filter(room -> RoomTypeDto.WORKSPACE.equals(room.getType())).findAny()
        .orElseThrow();
      assertEquals(5, workspace.getRank());
      assertEquals(2, workspace.getChildren().size());
      assertTrue(workspace.getChildren().stream().anyMatch(child -> RoomTypeDto.CHANNEL.equals(child.getType())));
      assertTrue(workspace.getChildren().stream().anyMatch(child -> child.getMembers().isEmpty()));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly gets the rooms list with user settings of authenticated user")
    public void listRoom_testOkWithUserSettings() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      UUID workspace1Id = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(room2Id, RoomTypeDto.GROUP, "room2",
        List.of(user1Id, user2Id), List.of(user1Id), List.of(user1Id), OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveRoom(workspace1Id, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel1")
        .description("Channel one")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(2), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel2")
        .description("Channel two")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(1), List.of());

      MockHttpResponse response = dispatcher.get(url(Map.of("extraFields", List.of("settings"))), user1Token);
      assertEquals(200, response.getStatus());
      assertFalse(response.getContentAsString().contains("members"));
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, rooms.size());

      RoomDto room1 = rooms.stream().filter(room -> room.getId().equals(room1Id)).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room1.getType());
      assertEquals(0, room1.getMembers().size());
      assertNotNull(room1.getUserSettings());
      assertNull(room1.getPictureUpdatedAt());
      assertNull(room1.getRank());
      assertTrue(room1.getChildren().isEmpty());

      RoomDto room2 = rooms.stream().filter(room -> room.getId().equals(room2Id)).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room2.getType());
      assertEquals(0, room2.getMembers().size());
      assertNotNull(room2.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room2.getPictureUpdatedAt());
      assertNull(room2.getRank());
      assertTrue((room2.getChildren().isEmpty()));

      RoomDto workspace = rooms.stream().filter(room -> room.getId().equals(workspace1Id)).findAny()
        .orElseThrow();
      assertEquals(RoomTypeDto.WORKSPACE, workspace.getType());
      assertEquals(0, workspace.getMembers().size());
      assertNull(workspace.getUserSettings());
      assertNull(workspace.getPictureUpdatedAt());
      assertEquals(5, workspace.getRank());
      assertEquals(2, workspace.getChildren().size());
      assertTrue(workspace.getChildren().stream().anyMatch(child -> RoomTypeDto.CHANNEL.equals(child.getType())));
      assertTrue(workspace.getChildren().stream().anyMatch(child -> child.getMembers().isEmpty()));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly gets the complete rooms list of authenticated user")
    public void listRoom_testOkCompleteRooms() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      UUID workspace1Id = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(room2Id, RoomTypeDto.GROUP, "room2",
        List.of(user1Id, user2Id), List.of(user1Id), List.of(user1Id), OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveRoom(workspace1Id, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel1")
        .description("Channel one")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(2), List.of(
        RoomMemberField.create().id(user1Id).muted(true)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel2")
        .description("Channel two")
        .hash(UUID.randomUUID().toString())
        .parentId(workspace1Id.toString())
        .rank(1), List.of(
        RoomMemberField.create().id(user1Id).muted(true)));

      MockHttpResponse response = dispatcher.get(url(Map.of("extraFields", List.of("members", "settings"))),
        user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, rooms.size());

      RoomDto room1 = rooms.stream().filter(room -> room1Id.equals(room.getId())).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room1.getType());
      assertNotNull(room1.getMembers());
      assertNotNull(room1.getUserSettings());
      assertNull(room1.getPictureUpdatedAt());

      RoomDto room2 = rooms.stream().filter(room -> room2Id.equals(room.getId())).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room2.getType());
      assertNotNull(room2.getMembers());
      assertNotNull(room2.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room2.getPictureUpdatedAt());

      RoomDto workspace = rooms.stream().filter(room -> workspace1Id.equals(room.getId())).findAny().orElseThrow();
      assertEquals(RoomTypeDto.WORKSPACE, workspace.getType());
      assertNotNull(workspace.getMembers());
      assertNull(workspace.getUserSettings());
      assertEquals(5, workspace.getRank());
      assertEquals(2, workspace.getChildren().size());
      assertTrue(workspace.getChildren().stream().anyMatch(child -> RoomTypeDto.CHANNEL.equals(child.getType())));
      assertTrue(workspace.getChildren().stream().anyMatch(child -> child.getMembers().isEmpty()));
      assertTrue(workspace.getChildren().stream().anyMatch(child -> child.getUserSettings().isMuted()));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly returns an empty list if the authenticated user isn't a member for any room")
    public void listRoom_testNoRooms() throws Exception {
      MockHttpResponse response = dispatcher.get(url(null), user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(0, rooms.size());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("If there isn't an authenticated user return a status code 401")
    public void listRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(null), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Insert room tests")
  public class InsertRoomTests {

    private static final String URL = "/rooms";

    @Nested
    @DisplayName("Insert group room tests")
    public class InsertGroupRoomTests {

      @Test
      @DisplayName("Given creation fields, inserts a new group room and returns its data")
      public void insertGroupRoom_testOk() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        mongooseImMockServer.mockCreateRoom(roomId.toString(), user1Id.toString(), "testRoom", "Test room", true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString(), true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user3Id.toString(), true);
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
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.GROUP, room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
          room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().orElseThrow()
            .isOwner());
        assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());

        mongooseImMockServer.verify(
          mongooseImMockServer.getCreateRoomRequest(roomId.toString(), user1Id.toString(), "testRoom", "Test room"),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user3Id.toString()),
          VerificationTimes.exactly(1));
        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given creation fields, if there aren't at least two member invitations returns a status code 400")
      public void insertGroupRoom_testErrorRequestWithLessThanTwoMemberInvitations() throws Exception {
        MockHttpResponse response = dispatcher.post(URL,
          getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.GROUP, List.of(user2Id)),
          user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getOutput().length);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

    }

    @Nested
    @DisplayName("Insert one-to-one room tests")
    public class InsertOneToOneRoomTests {

      @Test
      @DisplayName("Given creation fields, inserts a new one to one room and returns its data")
      public void insertOneToOneRoom_testOk() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("c9f83f1c-9b96-4731-9404-79e45a5d6d3c");
        mongooseImMockServer.mockCreateRoom(roomId.toString(), user1Id.toString(), "testOneToOne", "Test room", true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString(), true);
        mongooseImMockServer.mockAddUserToContacts(user2Id.toString(), user1Id.toString(), true);
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response = dispatcher.post(URL,
            getInsertRoomRequestBody("testOneToOne", "Test room", RoomTypeDto.ONE_TO_ONE, List.of(user2Id)),
            user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify("GET", String.format("/users/id/%s", user2Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testOneToOne", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.ONE_TO_ONE, room.getType());
        assertEquals(2, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
          room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().orElseThrow()
            .isOwner());
        assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());

        mongooseImMockServer.verify(
          mongooseImMockServer.getCreateRoomRequest(roomId.toString(), user1Id.toString(), "testOneToOne", "Test room"),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddUserToContactsRequest(user2Id.toString(), user1Id.toString()),
          VerificationTimes.exactly(1));
        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given creation fields for a one to one room, if there is a room with those users returns a status code 409")
      public void insertOneToOneRoom_testAlreadyExists() throws Exception {
        UUID roomId = UUID.randomUUID();
        integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, "testOneToOne",
          List.of(user1Id, user2Id));
        integrationTestUtils.generateAndSaveRoom(UUID.randomUUID(), RoomTypeDto.ONE_TO_ONE, "testOneToOne",
          List.of(user1Id, user3Id));
        integrationTestUtils.generateAndSaveRoom(UUID.randomUUID(), RoomTypeDto.ONE_TO_ONE, "testOneToOne",
          List.of(user2Id, user3Id));

        MockHttpResponse response = dispatcher.post(URL,
          getInsertRoomRequestBody("testOneToOne", "Test room", RoomTypeDto.ONE_TO_ONE, List.of(user2Id)),
          user1Token);
        assertEquals(409, response.getStatus());
        assertEquals(0, response.getOutput().length);
        mongooseImMockServer.verifyZeroInteractions();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

      @Test
      @DisplayName("Given one-to-one creation fields, if there are more then one invitation return a ststus code 400")
      public void insertOneToOneRoom_testMoreThenOneInvitation() throws Exception {
        MockHttpResponse response = dispatcher.post(URL,
          getInsertRoomRequestBody("testOneToOne", "Test room", RoomTypeDto.ONE_TO_ONE,
            List.of(user2Id, user3Id)), user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
        mongooseImMockServer.verifyZeroInteractions();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }
    }

    @Nested
    @DisplayName("Insert workspace room tests")
    public class InsertWorkspaceRoomTests {

      @Test
      @DisplayName("Given workspace creation fields, inserts first workspace room and returns its data")
      public void insertWorkspaceRoom_firstWorkspaceTestOk() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("6ef74886-3c81-492e-b6f1-3db4a59240e3");
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response = dispatcher.post(URL,
            getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.WORKSPACE, List.of(user2Id, user3Id)),
            user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify("GET", String.format("/users/id/%s", user2Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/users/id/%s", user3Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.WORKSPACE, room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
          room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().orElseThrow()
            .isOwner());
        assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
        assertEquals(1, room.getRank());
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());

        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given workspace creation fields, inserts nth workspace room and returns its data")
      public void insertWorkspaceRoom_nthWorkspaceTestOk() throws Exception {

        integrationTestUtils.generateAndSaveRoom(
          Room.create().id(UUID.randomUUID().toString()).name("workspace1").description("Workspace one")
            .type(RoomTypeDto.WORKSPACE).hash("hash"),
          List.of(
            RoomMemberField.create().id(user1Id).owner(true).rank(10),
            RoomMemberField.create().id(user2Id).rank(9),
            RoomMemberField.create().id(user3Id).rank(8)));

        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("6ef74886-3c81-492e-b6f1-3db4a59240e3");
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response = dispatcher.post(URL,
            getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.WORKSPACE, List.of(user2Id, user3Id)),
            user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify("GET", String.format("/users/id/%s", user2Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/users/id/%s", user3Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.WORKSPACE, room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
          room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().orElseThrow()
            .isOwner());
        assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
        assertEquals(11, room.getRank());
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());

        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given creation fields, if there aren't at least two member invitations returns a status code 400")
      public void insertWorkspaceRoom_testErrorRequestWithLessThanTwoMemberInvitations() throws Exception {
        MockHttpResponse response = dispatcher.post(URL,
          getInsertRoomRequestBody("testRoom", "Test room", RoomTypeDto.WORKSPACE, List.of(user2Id)),
          user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getOutput().length);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

    }

    @Nested
    @DisplayName("Insert channel room tests")
    public class InsertChannelRoomTests {

      @Test
      @DisplayName("Given channel creation fields, inserts first channel room in a workspace and returns its data")
      public void insertChannelRoom_firstChannelTestOk() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)
        ));

        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        mongooseImMockServer.mockCreateRoom(roomId.toString(), user1Id.toString(), "testRoom", "Test room", true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString(), true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user3Id.toString(), true);
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(workspaceId.toString())).thenReturn(workspaceId);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response = dispatcher.post(URL,
            objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
              .name("testRoom")
              .description("Test room")
              .type(RoomTypeDto.CHANNEL)
              .parentId(workspaceId)),
            user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.CHANNEL, room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
          room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().orElseThrow()
            .isOwner());
        assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
        assertEquals(1, room.getRank());
        assertEquals(workspaceId, room.getParentId());
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
        mongooseImMockServer.verify(
          mongooseImMockServer.getCreateRoomRequest(roomId.toString(), user1Id.toString(), "testRoom", "Test room"),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user3Id.toString()),
          VerificationTimes.exactly(1));
        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given channel creation fields, inserts nth channel room in a workspace and returns its data")
      public void insertChannelRoom_nthChannelTestOk() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)
        ));

        integrationTestUtils.generateAndSaveRoom(Room.create()
          .id(UUID.randomUUID().toString())
          .name("channel7")
          .description("Channel seven")
          .type(RoomTypeDto.CHANNEL)
          .hash(UUID.randomUUID().toString())
          .rank(7)
          .parentId(workspaceId.toString()), List.of());

        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        mongooseImMockServer.mockCreateRoom(roomId.toString(), user1Id.toString(), "testRoom", "Test room", true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString(), true);
        mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user3Id.toString(), true);
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(workspaceId.toString())).thenReturn(workspaceId);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response = dispatcher.post(URL,
            objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
              .name("testRoom")
              .description("Test room")
              .type(RoomTypeDto.CHANNEL)
              .parentId(workspaceId)),
            user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.CHANNEL, room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
          room.getMembers().stream().filter(member -> user1Id.equals(member.getUserId())).findAny().orElseThrow()
            .isOwner());
        assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
        assertEquals(8, room.getRank());
        assertEquals(workspaceId, room.getParentId());
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
        mongooseImMockServer.verify(
          mongooseImMockServer.getCreateRoomRequest(roomId.toString(), user1Id.toString(), "testRoom", "Test room"),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user3Id.toString()),
          VerificationTimes.exactly(1));
        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given channel creation fields, if there is at least a member returns a status code 400")
      public void insertChannelRoom_testErrorRequestWithMembers() throws Exception {
        MockHttpResponse response = dispatcher.post(URL,
          objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
            .name("testRoom")
            .description("Test room")
            .type(RoomTypeDto.CHANNEL)
            .parentId(UUID.randomUUID())
            .membersIds(List.of(user2Id, user3Id))), user1Token);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().isEmpty());
      }

      @Test
      @DisplayName("Given channel creation fields, if there isn't the parent identifier returns a status code 400")
      public void insertChannelRoom_testErrorRequestWithoutParentId() throws Exception {
        MockHttpResponse response = dispatcher.post(URL,
          objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
            .name("testRoom")
            .description("Test room")
            .type(RoomTypeDto.CHANNEL)), user1Token);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().isEmpty());
      }

      @Test
      @DisplayName("Given channel creation fields, if there isn't the requested workspace room returns a status code 404")
      public void insertChannelRoom_testErrorRequestedWorkspaceNotExists() throws Exception {
        MockHttpResponse response = dispatcher.post(URL,
          objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
            .name("testRoom")
            .description("Test room")
            .type(RoomTypeDto.CHANNEL)
            .parentId(UUID.randomUUID())), user1Token);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().isEmpty());
      }

      @Test
      @DisplayName("Given channel creation fields, if the authenticated user is not the workspace owner returns a status code 403")
      public void insertChannelRoom_testErrorAuthenticatedUserIsNotWorkspaceOwner() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)
        ));
        MockHttpResponse response = dispatcher.post(URL,
          objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
            .name("testRoom")
            .description("Test room")
            .type(RoomTypeDto.CHANNEL)
            .parentId(workspaceId)),
          user3Token);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().isEmpty());
      }

      @Test
      @DisplayName("Given channel creation fields, if the parent isn't a workspace returns a status code 400")
      public void insertChannelRoom_testErrorParentIsNotAWorkspace() throws Exception {
        UUID groupId = UUID.randomUUID();
        integrationTestUtils.generateAndSaveRoom(groupId, RoomTypeDto.GROUP, List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)
        ));
        MockHttpResponse response = dispatcher.post(URL,
          objectMapper.writeValueAsString(RoomCreationFieldsDto.create()
            .name("testRoom")
            .description("Test room")
            .type(RoomTypeDto.CHANNEL)
            .parentId(groupId)),
          user1Token);
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().isEmpty());
      }
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
    @DisplayName("Given a group room identifier, correctly returns the room information with members and user settings")
    public void getGroupRoom_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "testRoom",
        List.of(user1Id, user2Id, user3Id), List.of(user1Id), List.of(user1Id),
        OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(200, response.getStatus());
      RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
      assertEquals(roomId, room.getId());
      assertEquals("testRoom", room.getName());
      assertNotNull(room.getMembers());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
      assertNotNull(room.getUserSettings());
      assertTrue(room.getUserSettings().isMuted());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a workspace room identifier, correctly returns the room information with members, user settings and channels")
    public void getWorkspaceRoom_testOk() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
          .id(channel1Id.toString())
          .type(RoomTypeDto.CHANNEL)
          .name("channel1")
          .description("Channel one")
          .hash(UUID.randomUUID().toString())
          .parentId(workspaceId.toString())
          .rank(2),
        List.of(
          RoomMemberField.create().id(user1Id).muted(true),
          RoomMemberField.create().id(user3Id).muted(true)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
          .id(channel2Id.toString())
          .type(RoomTypeDto.CHANNEL)
          .name("channel2")
          .description("Channel two")
          .hash(UUID.randomUUID().toString())
          .parentId(workspaceId.toString())
          .rank(1),
        List.of(RoomMemberField.create().id(user2Id).muted(true)));

      MockHttpResponse response = dispatcher.get(url(workspaceId), user1Token);
      assertEquals(200, response.getStatus());
      RoomDto workspace = objectMapper.readValue(response.getContentAsString(), RoomDto.class);

      assertEquals(workspaceId, workspace.getId());
      assertEquals(RoomTypeDto.WORKSPACE, workspace.getType());
      assertEquals(5, workspace.getRank());

      assertNotNull(workspace.getMembers());
      assertEquals(3, workspace.getMembers().size());
      assertTrue(workspace.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
      assertTrue(workspace.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
      assertTrue(workspace.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));

      assertNull(workspace.getUserSettings());
      assertNotNull(workspace.getChildren());
      assertTrue(workspace.getChildren().stream().anyMatch(child -> RoomTypeDto.CHANNEL.equals(child.getType())));
      assertTrue(workspace.getChildren().stream().anyMatch(child -> workspaceId.equals(child.getParentId())));
      Optional<RoomDto> channel1 = workspace.getChildren().stream()
        .filter(child -> channel1Id.equals(child.getId())).findAny();
      assertTrue(channel1.isPresent());
      assertEquals(channel1Id, channel1.get().getId());
      assertEquals("channel1", channel1.get().getName());
      assertEquals(2, channel1.get().getRank());

      Optional<RoomDto> channel2 = workspace.getChildren().stream()
        .filter(child -> channel2Id.equals(child.getId())).findAny();
      assertTrue(channel2.isPresent());
      assertEquals(channel2Id, channel2.get().getId());
      assertEquals("channel2", channel2.get().getName());
      assertEquals(1, channel2.get().getRank());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Returns the required channel room with all members and room user settings")
    public void getRoomById_channelTestOk() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
          .id(channelId.toString())
          .type(RoomTypeDto.CHANNEL)
          .name("channel1")
          .description("Channel one")
          .hash(UUID.randomUUID().toString())
          .parentId(workspaceId.toString())
          .rank(8),
        List.of(
          RoomMemberField.create().id(user1Id).muted(true),
          RoomMemberField.create().id(user3Id).muted(true)));

      MockHttpResponse response = dispatcher.get(url(channelId), user1Token);
      assertEquals(200, response.getStatus());

      RoomDto channel = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
      assertEquals(channelId, channel.getId());
      assertEquals(8, channel.getRank());
      assertEquals(3, channel.getMembers().size());
      assertTrue(channel.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
      assertTrue(channel.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
      assertTrue(channel.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
      assertTrue(channel.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("Given a room without a picture, correctly returns the room information with members and user settings")
    public void getRoom_testOkWithoutPicture() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "testRoom",
        List.of(user1Id, user2Id, user3Id), List.of(user1Id), List.of(user1Id), null);

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(200, response.getStatus());
      RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
      assertEquals(roomId, room.getId());
      assertEquals("testRoom", room.getName());
      assertNotNull(room.getMembers());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
      assertTrue(room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
      assertNotNull(room.getUserSettings());
      assertTrue(room.getUserSettings().isMuted());
      assertNull(room.getPictureUpdatedAt());

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
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "testRoom", "Test room",
        List.of(user1Id, user2Id, user3Id), List.of(user1Id), List.of(user1Id),
        OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      mongooseImMockServer.mockSendStanza(roomId.toString(), user1Id.toString(), "roomNameChanged",
        Map.of("value", "updatedRoom"), true);
      mongooseImMockServer.mockSendStanza(roomId.toString(), user1Id.toString(), "roomDescriptionChanged",
        Map.of("value", "Updated room"), true);
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
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(
        mongooseImMockServer.getSendStanzaRequest(roomId.toString(), user1Id.toString(), "roomNameChanged",
          Map.of("value", "updatedRoom")), VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
        mongooseImMockServer.getSendStanzaRequest(roomId.toString(), user1Id.toString(), "roomDescriptionChanged",
          Map.of("value", "Updated room")), VerificationTimes.exactly(1));
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
  public class DeleteRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a group room identifier, correctly deletes the room")
    public void deleteRoom_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP,
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id).muted(true),
          RoomMemberField.create().id(user3Id)));
      mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

      // TODO: 23/02/22 verify event dispatcher interactions
      mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a group room identifier, correctly deletes the room and the associated meeting")
    public void deleteRoom_groupWithMeetingTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      UUID meetingId = UUID.randomUUID();
      Room room = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP,
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id).muted(true),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
        ParticipantBuilder.create(user1Id, "user3session1").audioStreamOn(false).videoStreamOn(false)));
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertTrue(meetingTestUtils.getParticipant(meetingId, "user3session1").isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
        VerificationTimes.exactly(1));
    }

    @Nested
    @DisplayName("Delete group room with attachments tests")
    public class DeleteGroupRoomWithAttachmentsTests {

      @Test
      @DisplayName("Correctly deletes the room and all associated attachments files")
      public void deleteGroupRoomWithAttachments_testOk() throws Exception {
        UUID roomId = UUID.randomUUID();
        String file1Id = UUID.randomUUID().toString();
        String file2Id = UUID.randomUUID().toString();
        integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP,
          List.of(
            RoomMemberField.create().id(user1Id).owner(true),
            RoomMemberField.create().id(user2Id).muted(true),
            RoomMemberField.create().id(user3Id)));
        fileMetadataRepository.save(
          FileMetadata.create().id(file1Id).type(FileMetadataType.ATTACHMENT).name("-").userId(user1Id.toString())
            .roomId(roomId.toString()).originalSize(0L).mimeType("-"));
        fileMetadataRepository.save(
          FileMetadata.create().id(file2Id).type(FileMetadataType.ATTACHMENT).name("-").userId(user1Id.toString())
            .roomId(roomId.toString()).originalSize(0L).mimeType("-"));
        storageMockServer.setBulkDeleteResponse(List.of(file1Id, file2Id), List.of(file1Id, file2Id));
        mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
        assertTrue(
          fileMetadataRepository.getIdsByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT).isEmpty());

        storageMockServer.verify(
          storageMockServer.getBulkDeleteRequest(List.of(file1Id, file2Id)), VerificationTimes.exactly(1));
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
          VerificationTimes.exactly(1));
      }

      @Test
      @DisplayName("Deletes the room and only attachments that storage service has deleted")
      public void deleteGroupRoomWithAttachments_storageServiceNotDeletesAllFiles() throws Exception {
        UUID roomId = UUID.randomUUID();
        String file1Id = UUID.randomUUID().toString();
        String file2Id = UUID.randomUUID().toString();
        integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP,
          List.of(
            RoomMemberField.create().id(user1Id).owner(true),
            RoomMemberField.create().id(user2Id).muted(true),
            RoomMemberField.create().id(user3Id)));
        fileMetadataRepository.save(
          FileMetadata.create().id(file1Id).type(FileMetadataType.ATTACHMENT).name("-").userId(user1Id.toString())
            .roomId(roomId.toString()).originalSize(0L).mimeType("-"));
        fileMetadataRepository.save(
          FileMetadata.create().id(file2Id).type(FileMetadataType.ATTACHMENT).name("-").userId(user1Id.toString())
            .roomId(roomId.toString()).originalSize(0L).mimeType("-"));
        storageMockServer.setBulkDeleteResponse(List.of(file1Id, file2Id), List.of(file1Id));
        mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
        assertTrue(
          fileMetadataRepository.getById(file1Id).isEmpty());
        Optional<FileMetadata> snoopy = fileMetadataRepository.getById(file2Id);
        assertTrue(snoopy.isPresent());
        assertNull(snoopy.get().getRoomId());

        storageMockServer.verify(
          storageMockServer.getBulkDeleteRequest(List.of(file1Id, file2Id)), VerificationTimes.exactly(1));
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
          VerificationTimes.exactly(1));
      }

      @Test
      @DisplayName("Deletes the room but no associated attachments files because storage service failed")
      public void deleteGroupRoomWithAttachments_storageServiceFailed() throws Exception {
        UUID roomId = UUID.randomUUID();
        String file1Id = UUID.randomUUID().toString();
        String file2Id = UUID.randomUUID().toString();
        integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP,
          List.of(
            RoomMemberField.create().id(user1Id).owner(true),
            RoomMemberField.create().id(user2Id).muted(true),
            RoomMemberField.create().id(user3Id)));
        fileMetadataRepository.save(
          FileMetadata.create().id(file1Id).type(FileMetadataType.ATTACHMENT).name("-").userId(user1Id.toString())
            .roomId(roomId.toString()).originalSize(0L).mimeType("-"));
        fileMetadataRepository.save(
          FileMetadata.create().id(file2Id).type(FileMetadataType.ATTACHMENT).name("-").userId(user1Id.toString())
            .roomId(roomId.toString()).originalSize(0L).mimeType("-"));
        storageMockServer.setBulkDeleteResponse(List.of(file1Id, file2Id), null);
        mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());

        Optional<FileMetadata> charlie = fileMetadataRepository.getById(file1Id);
        assertTrue(charlie.isPresent());
        assertNull(charlie.get().getRoomId());

        Optional<FileMetadata> snoopy = fileMetadataRepository.getById(file2Id);
        assertTrue(snoopy.isPresent());
        assertNull(snoopy.get().getRoomId());

        storageMockServer.verify(
          storageMockServer.getBulkDeleteRequest(List.of(file1Id, file2Id)), VerificationTimes.exactly(1));
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
          VerificationTimes.exactly(1));
      }
    }

    @Test
    @DisplayName("Given a workspace identifier, correctly delete the workspace without channel")
    public void deleteRoom_workspaceWithoutChannelTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.WORKSPACE,
        List.of(
          RoomMemberField.create().id(user1Id).owner(true).rank(10),
          RoomMemberField.create().id(user2Id).muted(true).rank(9),
          RoomMemberField.create().id(user3Id).rank(8)));

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

      // TODO: 23/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName("Given a workspace room identifier, correctly delete the workspace with channel")
    public void deleteRoom_workspaceWithChannelTestOk() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE,
        List.of(
          RoomMemberField.create().id(user1Id).owner(true).rank(10),
          RoomMemberField.create().id(user2Id).muted(true).rank(9),
          RoomMemberField.create().id(user3Id).rank(8)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel1")
        .description("Channel one")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(1), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel2")
        .description("Channel two")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(2), List.of());
      mongooseImMockServer.mockDeleteRoom(channel1Id.toString(), true);
      mongooseImMockServer.mockDeleteRoom(channel2Id.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(workspaceId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(workspaceId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(workspaceId.toString()).isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(channel1Id.toString()),
        VerificationTimes.exactly(1));
      mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(channel2Id.toString()),
        VerificationTimes.exactly(1));
      // TODO: 23/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName("Given a channel room identifier, correctly delete the room")
    public void deleteRoom_ChannelTestOk() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE,
        List.of(
          RoomMemberField.create().id(user1Id).owner(true).rank(10),
          RoomMemberField.create().id(user2Id).muted(true).rank(9),
          RoomMemberField.create().id(user3Id).rank(8)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channelId.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("testChannel")
        .description("Channel test")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(1), List.of());
      mongooseImMockServer.mockDeleteRoom(channelId.toString(), true);
      MockHttpResponse response = dispatcher.delete(url(channelId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(channelId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(channelId.toString()).isEmpty());
      assertTrue(integrationTestUtils.getRoomById(workspaceId).orElseThrow().getChildren().isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(mongooseImMockServer.getDeleteRoomRequest(channelId.toString()),
        VerificationTimes.exactly(1));
      // TODO: 23/02/22 verify event dispatcher interactions
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
    @DisplayName("Given a room identifier, if the storage hasn't the picture file returns status code 424")
    public void getRoomPicture_testErrorStorageHasNoPictureFile() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(roomId, FileMetadataType.ROOM_AVATAR, user1Id, roomId);
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(424, response.getStatus());
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
      mongooseImMockServer.mockSendStanza(roomId.toString(), user1Id.toString(), "roomPictureUpdated",
        Map.of("picture-id", roomId.toString(), "picture-name", fileMock.getName()), true);
      Instant now = Instant.now();
      clock.fixTimeAt(now);
      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getId().getBytes(),
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user1Token);
      mongooseImMockServer.verify(
        mongooseImMockServer.getSendStanzaRequest(roomId.toString(), user1Id.toString(), "roomPictureUpdated",
          Map.of("picture-id", roomId.toString(), "picture-name", fileMock.getName())));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      // TODO: 01/03/22 verify event dispatcher iterations
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      Optional<Room> room = roomRepository.getById(roomId.toString());
      assertTrue(room.isPresent());
      assertEquals(OffsetDateTime.ofInstant(now, clock.getZone()).toEpochSecond(),
        room.get().getUpdatedAt().toEpochSecond());

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
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, if user is not a room owner returns status code 403")
    public void updateRoomPicture_testErrorUserIsNotRoomOwner() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), fileMock.getFileBytes(),
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user3Token);
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
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user1Token);
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
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user1Token);
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
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Delete room picture tests")
  public class DeleteRoomPictureTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/picture", roomId);
    }

    @Test
    @DisplayName("Correctly deletes the room picture")
    public void deleteRoomPicture_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id),
        List.of(user1Id), null, OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);
      mongooseImMockServer.mockSendStanza(roomId.toString(), user1Id.toString(), "roomPictureDeleted", Map.of(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertTrue(integrationTestUtils.getFileMetadataById(fileMock.getUUID()).isEmpty());
      assertNull(integrationTestUtils.getRoomById(roomId).orElseThrow().getPictureUpdatedAt());

      mongooseImMockServer.verify(
        mongooseImMockServer.getSendStanzaRequest(roomId.toString(), user1Id.toString(), "roomPictureDeleted",
          Map.of()), VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("DELETE", "/delete", fileMock.getId(), 1);
    }

    @Test
    @DisplayName("If the user is not authenticated, it returns status code 401")
    public void deleteRoomPicture_unauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If user is not a room owner, it returns status code 403")
    public void deleteRoomPicture_userNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1",
        List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the room hasn't a picture, it returns a status code 404")
    public void deleteRoomPicture_fileNotFound() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertTrue(integrationTestUtils.getFileMetadataById(fileMock.getUUID()).isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Mutes room for authenticated user tests")
  public class MutesRoomForAuthenticatedUserTests {

    private final OffsetDateTime MUTED_TO_INFINITY = OffsetDateTime.parse("0001-01-01T00:00:00Z");

    private String url(UUID roomId) {
      return String.format("/rooms/%s/mute", roomId);
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings not exists")
    public void muteRoom_testOkUserSettingNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(MUTED_TO_INFINITY.toInstant(),
        roomUserSettings.get().getMutedUntil().toInstant());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings exists")
    public void muteRoom_testOkUserSettingExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(RoomUserSettings.create(room, user3Id.toString()));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(MUTED_TO_INFINITY.toInstant(),
        roomUserSettings.get().getMutedUntil().toInstant());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Correctly does nothing if the user is already muted")
    public void muteRoom_testOkUserAlreadyMuted() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(
        RoomUserSettings.create(room, user3Id.toString()).mutedUntil(MUTED_TO_INFINITY));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(MUTED_TO_INFINITY.toInstant(),
        roomUserSettings.get().getMutedUntil().toInstant());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Correctly mute a channel")
    public void muteRoom_testOkChannel() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(1),
        RoomMemberField.create().id(user2Id).rank(2),
        RoomMemberField.create().id(user3Id).rank(3)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channelId.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel")
        .description("Channel test")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(1), List.of());

      MockHttpResponse response = dispatcher.put(url(channelId), null, user1Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(channelId, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertNotNull(roomUserSettings.get().getMutedUntil());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("If the room is a workspace, it responds with status code 400")
    public void muteRoom_errorRoomIsWorkspace() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(1),
        RoomMemberField.create().id(user2Id).rank(2),
        RoomMemberField.create().id(user3Id).muted(true).rank(3)
      ));

      MockHttpResponse response = dispatcher.put(url(workspaceId), null, user1Token);

      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void muteRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getContentAsString().length());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void muteRoom_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), null, user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getContentAsString().length());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Unmute room for authenticated user tests")
  public class UnmuteRoomForAuthenticatedUserTests {

    private final OffsetDateTime MUTED_TO_INFINITY = OffsetDateTime.parse("0001-01-01T00:00:00Z");

    private String url(UUID roomId) {
      return String.format("/rooms/%s/mute", roomId);
    }

    @Test
    @DisplayName("Correctly does nothing if user settings not exists")
    public void unmuteRoom_testOkUserSettingNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertFalse(roomUserSettings.isPresent());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Unmute the current user in a specific room")
    public void unmuteRoom_testOkUserSettingExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(
        RoomUserSettings.create(room, user3Id.toString()).mutedUntil(MUTED_TO_INFINITY));
      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertNull(roomUserSettings.get().getMutedUntil());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Correctly does nothing if the user has already unmuted")
    public void unmuteRoom_testOkUserAlreadyUnmuted() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(RoomUserSettings.create(room, user3Id.toString()));
      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertNull(roomUserSettings.get().getMutedUntil());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Correctly unmute a channel")
    public void unmuteRoom_testOkChannel() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).muted(true).rank(1),
        RoomMemberField.create().id(user2Id).rank(2),
        RoomMemberField.create().id(user3Id).muted(true).rank(3)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channelId.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel")
        .description("Channel test")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(1), List.of(RoomMemberField.create().id(user1Id).muted(true)));

      MockHttpResponse response = dispatcher.delete(url(channelId), user1Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(channelId, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertNull(roomUserSettings.get().getMutedUntil());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("If the room is a workspace, it responds with status code 400")
    public void unmuteRoom_errorRoomIsWorkspace() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).muted(true).rank(1),
        RoomMemberField.create().id(user2Id).rank(2),
        RoomMemberField.create().id(user3Id).muted(true).rank(3)
      ));

      MockHttpResponse response = dispatcher.delete(url(workspaceId), user1Token);

      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void unmuteRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getContentAsString().length());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void unmuteRoom_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getContentAsString().length());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Clear room for authenticated user tests")
  public class ClearRoomForAuthenticatedUserTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/clear", roomId);
    }

    @Test
    @DisplayName("Sets the clear date to now when user settings doesn't exist")
    public void clearRoom_testOkUserSettingNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(200, response.getStatus());
      assertNotNull(objectMapper.readValue(response.getContentAsString(), ClearedDateDto.class).getClearedAt());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Sets the clear date to now when user settings exists")
    public void clearRoom_testOkUserSettingExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      OffsetDateTime prevDate = OffsetDateTime.parse("2022-01-01T00:00:00Z");
      integrationTestUtils.setRoomUserSettings(RoomUserSettings.create(room, user3Id.toString()).clearedAt(prevDate));
      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(200, response.getStatus());
      assertTrue(
        prevDate.isBefore(objectMapper.readValue(response.getContentAsString(), ClearedDateDto.class).getClearedAt()));
      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void clearRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getContentAsString().length());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void clearRoom_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), null, user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getContentAsString().length());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
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
  @DisplayName("Insert a room member tests")
  public class InsertRoomMemberTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/members", roomId);
    }

    @Test
    @DisplayName("Given a group room identifier, correctly insert the new room member")
    public void insertRoomMember_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id));
      mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user4Id.toString(), true);

      MemberToInsertDto requestMember = MemberToInsertDto.create().userId(user4Id).historyCleared(false);
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(requestMember), user1Token);

      assertEquals(201, response.getStatus());
      MemberInsertedDto member = objectMapper.readValue(response.getContentAsString(), MemberInsertedDto.class);
      assertEquals(requestMember.getUserId(), member.getUserId());
      assertNull(member.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      mongooseImMockServer.verify(
        mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user4Id.toString()),
        VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room is one to one returns status code 400")
    public void insertRoomMember_oneToOneTest() throws Exception {
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
    @DisplayName("Given a workspace room identifier, correctly insert the new room member")
    public void insertRoomMember_workspaceTestOk() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channel1Id = UUID.randomUUID();
      UUID channel2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(5),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(2)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel1")
        .description("Channel one")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(2), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel2")
        .description("Channel two")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(1), List.of());
      integrationTestUtils.generateAndSaveRoom(UUID.randomUUID(), RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user4Id).rank(5)
      ));
      mongooseImMockServer.mockAddRoomMember(workspaceId.toString(), user1Id.toString(), user4Id.toString(), true);
      MemberToInsertDto requestMember = MemberToInsertDto.create().userId(user4Id).historyCleared(false);
      MockHttpResponse response = dispatcher.post(url(workspaceId),
        getInsertRoomMemberRequestBody(requestMember), user1Token);

      assertEquals(201, response.getStatus());
      MemberInsertedDto member = objectMapper.readValue(response.getContentAsString(), MemberInsertedDto.class);
      assertEquals(requestMember.getUserId(), member.getUserId());
      assertNull(member.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(workspaceId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      Optional<RoomUserSettings> userSettings = roomUserSettingsRepository.getByRoomIdAndUserId(workspaceId.toString(),
        user4Id.toString());
      assertTrue(userSettings.isPresent());
      assertEquals(6, userSettings.get().getRank());

      mongooseImMockServer.verify(
        mongooseImMockServer.getAddRoomMemberRequest(channel1Id.toString(), user1Id.toString(), user4Id.toString()),
        VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
        mongooseImMockServer.getAddRoomMemberRequest(channel2Id.toString(), user1Id.toString(), user4Id.toString()),
        VerificationTimes.exactly(1));

      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room is a channel returns status code 400")
    public void insertRoomMember_channelTest() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(4),
        RoomMemberField.create().id(user2Id).rank(7)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channelId.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel")
        .description("Channel test")
        .hash(UUID.randomUUID().toString())
        .parentId(workspaceId.toString())
        .rank(2), List.of());

      MockHttpResponse response = dispatcher.post(url(channelId),
        getInsertRoomMemberRequestBody(user4Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a group room identifier, correctly insert the new room member clearing the history")
    public void insertRoomMember_historyClearedOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id));
      mongooseImMockServer.mockAddRoomMember(roomId.toString(), user1Id.toString(), user4Id.toString(), true);

      MemberToInsertDto requestMember = MemberToInsertDto.create().userId(user4Id).historyCleared(true);
      MockHttpResponse response = dispatcher.post(url(roomId),
        getInsertRoomMemberRequestBody(requestMember), user1Token);

      assertEquals(201, response.getStatus());
      MemberInsertedDto member = objectMapper.readValue(response.getContentAsString(), MemberInsertedDto.class);
      assertEquals(requestMember.getUserId(), member.getUserId());
      assertNotNull(member.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(roomId, user4Id);
      assertTrue(roomUserSettings.isPresent());
      assertNotNull(roomUserSettings.get().getClearedAt());

      mongooseImMockServer.verify(
        mongooseImMockServer.getAddRoomMemberRequest(roomId.toString(), user1Id.toString(), user4Id.toString()),
        VerificationTimes.exactly(1));

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
    @DisplayName("Given a room identifier, if the user isn't an account returns status code 404")
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

    @Test
    @DisplayName("Given a group room identifier, if the request doesn't contain historyCleared returns status code 500")
    public void insertRoomMember_testHistoryClearedNotInitialized() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user1Token);

      assertEquals(500, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());

      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }


    private String getInsertRoomMemberRequestBody(MemberToInsertDto member) {
      return getInsertRoomMemberRequestBody(
        member.getUserId(), member.isOwner(), member.isTemporary(), member.isExternal(), member.isHistoryCleared());
    }

    private String getInsertRoomMemberRequestBody(@Nullable UUID userId) {
      return getInsertRoomMemberRequestBody(userId, null, null, null, null);
    }

    private String getInsertRoomMemberRequestBody(
      @Nullable UUID userId, @Nullable Boolean owner, @Nullable Boolean temporary, @Nullable Boolean external,
      @Nullable Boolean historyCleared
    ) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("{");
      Optional.ofNullable(userId).ifPresent(u -> stringBuilder.append(String.format("\"userId\": \"%s\",", u)));
      Optional.ofNullable(owner).ifPresent(o -> stringBuilder.append(String.format("\"owner\": %s,", o)));
      Optional.ofNullable(temporary).ifPresent(t -> stringBuilder.append(String.format("\"temporary\": %s,", t)));
      Optional.ofNullable(external).ifPresent(e -> stringBuilder.append(String.format("\"external\": %s, ", e)));
      Optional.ofNullable(historyCleared)
        .ifPresent(e -> stringBuilder.append(String.format("\"historyCleared\": %s", e)));
      if (',' == stringBuilder.charAt(stringBuilder.length() - 1)) {
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      }
      stringBuilder.append("}");
      return stringBuilder.toString();
    }
  }

  @Nested
  @DisplayName("Remove a member from the room tests")
  public class RemoveRoomMemberTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s", roomId, userId);
    }

    @Test
    @DisplayName("Given a group room identifier and a member identifier, correctly remove the user from room members")
    public void deleteRoomMember_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
        .isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
        mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
        VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a group room identifier and a member identifier, " +
      "correctly leaves all user sessions from the associated meeting and removes the user from room members")
    public void deleteRoomMember_memberIsMeetingParticipantTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      UUID meetingId = UUID.randomUUID();
      Room roomEntity = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
        ParticipantBuilder.create(user1Id, "user1session1"),
        ParticipantBuilder.create(user2Id, "user2session1"),
        ParticipantBuilder.create(user2Id, "user2session2"),
        ParticipantBuilder.create(user3Id, "user3session1")));
      integrationTestUtils.updateRoom(roomEntity.meetingId(meetingId.toString()));
      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
        .isEmpty());
      Optional<Meeting> meeting = meetingTestUtils.getMeetingById(meetingId);
      assertTrue(meeting.isPresent());
      assertEquals(2, meeting.get().getParticipants().size());
      assertTrue(meeting.get().getParticipants().stream().noneMatch(participant ->
        user2Id.toString().equals(participant.getUserId())));

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
        mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
        VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a group room identifier and a member identifier, " +
      "correctly leaves the user session from the associated meeting, " +
      "removes the meeting because the user session was the last and removes the user from room members")
    public void deleteRoomMember_memberIsLastMeetingParticipantTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      UUID meetingId = UUID.randomUUID();
      Room roomEntity = integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room",
        List.of(user1Id, user2Id, user3Id));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
        ParticipantBuilder.create(user2Id, "user2session1")));
      integrationTestUtils.updateRoom(roomEntity.meetingId(meetingId.toString()));

      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
        .isEmpty());
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
        mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
        VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a workspace room identifier and a member identifier, correctly remove the user from room members")
    public void deleteRoomMember_workspaceTestOk() throws Exception {
      List.of(1, 2, 4, 5).forEach(index ->
        integrationTestUtils.generateAndSaveRoom(
          Room.create()
            .id(UUID.randomUUID().toString())
            .name("workspace")
            .description("Workspace")
            .type(RoomTypeDto.WORKSPACE)
            .hash("workspace" + index + "Hash"),
          List.of(RoomMemberField.create().id(user2Id).owner(true).rank(index))));

      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
        Room.create()
          .id(roomId.toString())
          .name("workspace")
          .description("Workspace")
          .type(RoomTypeDto.WORKSPACE)
          .hash("workspaceHash"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true).rank(1),
          RoomMemberField.create().id(user2Id).owner(true).rank(3),
          RoomMemberField.create().id(user3Id).owner(true).rank(5)));

      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
        .isEmpty());
      assertEquals(4, roomUserSettingsRepository.getByUserId(user2Id.toString()).size());

      // TODO: 25/02/22 verify event dispatcher
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
    @DisplayName("Given a room identifier and a member identifier, if the room is a one to one returns status code 403")
    public void deleteRoomMember_testErrorRoomOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, "room",
        List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a group room identifier and a member identifier equals to authenticated user, correctly user remove itself")
    public void deleteRoomMember_userRemoveItselfTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user3Id.toString(), true);
      MockHttpResponse response = dispatcher.delete(url(roomId, user3Id), user3Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user3Id.toString())).findAny()
        .isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
        mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user3Id.toString()));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Promote a member to owner tests")
  public class PromoteMemberToOwnerTests {

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
        room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
          .orElseThrow().isOwner());

      // TODO: 25/02/22 verify event dispatcher interactions
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

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the room is a one-to-one then it returns status code 400")
    public void updateToOwner_testErrorRoomIsOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, List.of(
        RoomMemberField.create().id(user1Id).owner(true),
        RoomMemberField.create().id(user2Id).owner(true)
      ));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the room is a channel then it returns status code 400")
    public void updateToOwner_testErrorRoomIsChannel() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(1),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(3)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channelId.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel")
        .description("Channel test")
        .hash(UUID.randomUUID().toString())
        .rank(1)
        .parentId(workspaceId.toString()), List.of());
      MockHttpResponse response = dispatcher.put(url(channelId, user2Id), null, user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Demote a member from owner to normal member tests")
  public class DemoteOwnerTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s/owner", roomId, userId);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, correctly demotes a owner to normal member")
    public void deleteOwner_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id),
        List.of(user1Id, user2Id, user3Id), List.of(), null);
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertFalse(
        room.get().getSubscriptions().stream().filter(s -> s.getUserId().equals(user2Id.toString())).findAny()
          .orElseThrow()
          .isOwner());

      // TODO: 28/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
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
        List.of(user2Id, user3Id), List.of(), null);
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

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the room is a one-to-one then it returns status code 400")
    public void deleteOwner_testErrorRoomIsOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.ONE_TO_ONE, List.of(
        RoomMemberField.create().id(user1Id).owner(true),
        RoomMemberField.create().id(user2Id).owner(true)
      ));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier and a member identifier, if the room is a channel then it returns status code 400")
    public void deleteOwner_testErrorRoomIsChannel() throws Exception {
      UUID workspaceId = UUID.randomUUID();
      UUID channelId = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE, List.of(
        RoomMemberField.create().id(user1Id).owner(true).rank(1),
        RoomMemberField.create().id(user2Id).owner(false).rank(2),
        RoomMemberField.create().id(user3Id).owner(false).rank(3)
      ));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channelId.toString())
        .type(RoomTypeDto.CHANNEL)
        .name("channel")
        .description("Channel test")
        .hash(UUID.randomUUID().toString())
        .rank(1)
        .parentId(workspaceId.toString()), List.of());
      MockHttpResponse response = dispatcher.delete(url(channelId, user2Id), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Gets paged list of room attachment information tests")
  public class ListRoomAttachmentInformationTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/attachments", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns a single paged list of attachments info of the required room")
    public void listRoomAttachmentInfo_testOkSinglePage() throws Exception {

      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("faec1132-567d-451c-a969-18ca9131bdfa"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("6a6a1f06-0947-4b5f-a6ac-7631426e3a62"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("991b5178-1108-459e-a017-4197647167ec"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      FileMetadata file4 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("5a3d8dd2-f431-4195-acc1-5108948c6d26"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.removeFixTime();

      MockHttpResponse response = dispatcher.get(url(room1Id), user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto attachments = objectMapper.readValue(response.getContentAsString(),
        new TypeReference<>() {
        });
      assertEquals(4, attachments.getAttachments().size());
      assertEquals(
        List.of(file3.getId(), file4.getId(), file1.getId(), file2.getId()),
        attachments.getAttachments().stream()
          .map(attachment -> attachment.getId().toString())
          .collect(Collectors.toList()));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns multiple paged lists of attachments info of the required room")
    public void listRoomAttachmentInfo_testOkMultiplePages() throws Exception {

      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("faec1132-567d-451c-a969-18ca9131bdfa"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("6a6a1f06-0947-4b5f-a6ac-7631426e3a62"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("991b5178-1108-459e-a017-4197647167ec"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2021-07-05T00:00:00Z"));
      FileMetadata file4 = integrationTestUtils.generateAndSaveFileMetadata(
        UUID.fromString("5a3d8dd2-f431-4195-acc1-5108948c6d26"),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.removeFixTime();

      MockHttpResponse response1 = dispatcher.get(
        String.join("", url(room1Id), "?itemsNumber=2"), user1Token);
      assertEquals(200, response1.getStatus());
      AttachmentsPaginationDto attachmentsPage1 = objectMapper.readValue(response1.getContentAsString(),
        AttachmentsPaginationDto.class);
      assertEquals(2, attachmentsPage1.getAttachments().size());
      assertEquals(
        List.of(file3.getId(), file1.getId()),
        attachmentsPage1.getAttachments().stream()
          .map(attachment -> attachment.getId().toString())
          .collect(Collectors.toList()));
      assertNotNull(attachmentsPage1.getFilter());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

      MockHttpResponse response2 = dispatcher.get(
        String.join("", url(room1Id), "?itemsNumber=2&filter=", attachmentsPage1.getFilter()), user1Token);
      assertEquals(200, response2.getStatus());
      AttachmentsPaginationDto attachmentsPage2 = objectMapper.readValue(response2.getContentAsString(),
        AttachmentsPaginationDto.class);
      assertEquals(2, attachmentsPage2.getAttachments().size());
      assertEquals(
        List.of(file4.getId(), file2.getId()),
        attachmentsPage2.getAttachments().stream()
          .map(attachment -> attachment.getId().toString())
          .collect(Collectors.toList()));
      assertNull(attachmentsPage2.getFilter());
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
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response = dispatcher.post(url(roomId), fileMock.getFileBytes(),
          Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
            String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
              fileMock.getMimeType())), user1Token);
      }

      assertEquals(201, response.getStatus());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
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
        Map.of("Content-Type", "application/octet-stream", "X-Content-Disposition",
          String.format("fileName=%s;mimeType=%s", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
            fileMock.getMimeType())), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Update workspaces rank tests")
  public class UpdateWorkspacesRankTests {

    private static final String URL = "/rooms/workspaces/rank";

    @Test
    @DisplayName("Given a pair list of room id and rank, correctly update workspace rank for the authenticated user")
    public void updateWorkspacesRank_testOk() throws Exception {
      UUID ws1Id = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID ws2Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID ws3Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");

      integrationTestUtils.generateAndSaveRoom(ws1Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(1)));
      integrationTestUtils.generateAndSaveRoom(ws2Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(9)));
      integrationTestUtils.generateAndSaveRoom(ws3Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(5)));

      MockHttpResponse response = dispatcher.put(URL, objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(ws1Id).rank(3),
        RoomRankDto.create().roomId(ws2Id).rank(2),
        RoomRankDto.create().roomId(ws3Id).rank(1))), user1Token);
      assertEquals(204, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());

      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(ws1Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(3, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws2Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(2, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws3Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(1, roomUserSettings.get().getRank());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if there authenticated user workspaces are not compatible "
      + "then return a status code 400")
    public void updateWorkspacesRank_testWorkspaceNotCompatibleWithList() throws Exception {
      UUID ws1Id = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID ws2Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID ws3Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");
      UUID ws4Id = UUID.fromString("deb5b4be-e2cf-487e-b089-6b0bc4dd213a");
      integrationTestUtils.generateAndSaveRoom(ws1Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(5)));
      integrationTestUtils.generateAndSaveRoom(ws2Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(8)));
      integrationTestUtils.generateAndSaveRoom(ws3Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(13)));

      MockHttpResponse response = dispatcher.put(URL, objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(ws4Id).rank(3),
        RoomRankDto.create().roomId(ws2Id).rank(2),
        RoomRankDto.create().roomId(ws1Id).rank(1))), user1Token);

      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(ws1Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(5, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws2Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(8, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws3Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(13, roomUserSettings.get().getRank());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if there authenticated user workspaces number is different "
      + "then return a status code 400")
    public void updateWorkspacesRank_testWorkspaceNumberDifferentThenList() throws Exception {
      UUID ws1Id = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID ws2Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID ws3Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");
      integrationTestUtils.generateAndSaveRoom(ws1Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(5)));
      integrationTestUtils.generateAndSaveRoom(ws2Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(8)));
      integrationTestUtils.generateAndSaveRoom(ws3Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(13)));

      MockHttpResponse response = dispatcher.put(URL, objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(ws2Id).rank(2),
        RoomRankDto.create().roomId(ws1Id).rank(1))), user1Token);

      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(ws1Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(5, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws2Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(8, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws3Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(13, roomUserSettings.get().getRank());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if its ranks are not progressive number sequencethere "
      + "then return a status code 400")
    public void updateWorkspacesRank_testRankListNotProgressiveNumberSequence() throws Exception {
      UUID ws1Id = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID ws2Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID ws3Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");
      integrationTestUtils.generateAndSaveRoom(ws1Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(5)));
      integrationTestUtils.generateAndSaveRoom(ws2Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(8)));
      integrationTestUtils.generateAndSaveRoom(ws3Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(13)));

      MockHttpResponse response = dispatcher.put(URL, objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(ws1Id).rank(7),
        RoomRankDto.create().roomId(ws2Id).rank(9),
        RoomRankDto.create().roomId(ws3Id).rank(1))), user1Token);

      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(ws1Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(5, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws2Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(8, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws3Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(13, roomUserSettings.get().getRank());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if it has duplicated room identifiers "
      + "then return a status code 400")
    public void updateWorkspacesRank_testRankListHasDuplicatedWorkspaceId() throws Exception {
      UUID ws1Id = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID ws2Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      integrationTestUtils.generateAndSaveRoom(ws1Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(5)));
      integrationTestUtils.generateAndSaveRoom(ws2Id, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).rank(8)));

      MockHttpResponse response = dispatcher.put(URL, objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(ws2Id).rank(3),
        RoomRankDto.create().roomId(ws2Id).rank(2),
        RoomRankDto.create().roomId(ws1Id).rank(1))), user1Token);

      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      Optional<RoomUserSettings> roomUserSettings = integrationTestUtils.getRoomUserSettings(ws1Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(5, roomUserSettings.get().getRank());
      roomUserSettings = integrationTestUtils.getRoomUserSettings(ws2Id, user1Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(8, roomUserSettings.get().getRank());
    }
  }

  @Nested
  @DisplayName("Updates channels rank tests")
  public class UpdateChannelsRankTests {

    private String url(UUID workspaceId) {
      return String.format("/rooms/workspaces/%s/channels/rank", workspaceId);
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, correctly update channels rank for workspace")
    public void updateChannelsRank_testOk() throws Exception {
      UUID workspaceId = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID channel1Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID channel2Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");
      UUID channel3Id = UUID.fromString("85184f58-a5a7-4fc5-a631-d2929e524a0f");
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).owner(true).rank(1)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .name("channel1")
        .description("Channel one")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(11)
        .parentId(workspaceId.toString()), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .name("channel2")
        .description("Channel two")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(7)
        .parentId(workspaceId.toString()), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel3Id.toString())
        .name("channel3")
        .description("Channel three")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(9)
        .parentId(workspaceId.toString()), List.of());

      MockHttpResponse response = dispatcher.put(url(workspaceId), objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(channel1Id).rank(1),
        RoomRankDto.create().roomId(channel2Id).rank(2),
        RoomRankDto.create().roomId(channel3Id).rank(3))), user1Token);
      assertEquals(204, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      Room workspace = integrationTestUtils.getRoomById(workspaceId).orElseThrow();
      assertEquals(3, workspace.getChildren().size());
      assertEquals(1, workspace.getChildren().stream()
        .filter(child -> channel1Id.toString().equals(child.getId()))
        .findAny().orElseThrow().getRank());
      assertEquals(2, workspace.getChildren().stream()
        .filter(child -> channel2Id.toString().equals(child.getId()))
        .findAny().orElseThrow().getRank());
      assertEquals(3, workspace.getChildren().stream()
        .filter(child -> channel3Id.toString().equals(child.getId()))
        .findAny().orElseThrow().getRank());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if there are rooms not compatible with the workspace channels "
      + "then return a status code 400")
    public void updateChannelsRank_testChannelNotCompatibleWithList() throws Exception {
      UUID workspaceId = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID channel1Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID channel2Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");
      UUID channel3Id = UUID.fromString("85184f58-a5a7-4fc5-a631-d2929e524a0f");
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).owner(true).rank(1)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .name("channel1")
        .description("Channel one")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(11)
        .parentId(workspaceId.toString()), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .name("channel2")
        .description("Channel two")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(7)
        .parentId(workspaceId.toString()), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel3Id.toString())
        .name("channel3")
        .description("Channel three")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(9)
        .parentId(workspaceId.toString()), List.of());

      MockHttpResponse response = dispatcher.put(url(workspaceId), objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(channel1Id).rank(1),
        RoomRankDto.create().roomId(channel2Id).rank(2),
        RoomRankDto.create().roomId(UUID.randomUUID()).rank(3))), user1Token);
      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
      Room workspace = integrationTestUtils.getRoomById(workspaceId).orElseThrow();
      assertEquals(3, workspace.getChildren().size());
      assertEquals(11, workspace.getChildren().stream()
        .filter(child -> channel1Id.toString().equals(child.getId()))
        .findAny().orElseThrow().getRank());
      assertEquals(7, workspace.getChildren().stream()
        .filter(child -> channel2Id.toString().equals(child.getId()))
        .findAny().orElseThrow().getRank());
      assertEquals(9, workspace.getChildren().stream()
        .filter(child -> channel3Id.toString().equals(child.getId()))
        .findAny().orElseThrow().getRank());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if the workspace channels number is different "
      + "then return a status code 400")
    public void updateChannelsRank_testWorkspaceChannelNumberDifferentThenList() throws Exception {
      UUID workspaceId = UUID.fromString("471276a4-33f5-44c5-90b9-dd198c9330ae");
      UUID channel1Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      UUID channel2Id = UUID.fromString("bff64789-8f16-4b6d-95fa-69505d63cbd4");
      integrationTestUtils.generateAndSaveRoom(workspaceId, RoomTypeDto.WORKSPACE,
        List.of(RoomMemberField.create().id(user1Id).owner(true).rank(1)));
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel1Id.toString())
        .name("channel1")
        .description("Channel one")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(11)
        .parentId(workspaceId.toString()), List.of());
      integrationTestUtils.generateAndSaveRoom(Room.create()
        .id(channel2Id.toString())
        .name("channel2")
        .description("Channel two")
        .type(RoomTypeDto.CHANNEL)
        .hash(UUID.randomUUID().toString())
        .rank(7)
        .parentId(workspaceId.toString()), List.of());

      MockHttpResponse response = dispatcher.put(url(workspaceId), objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(channel1Id).rank(1),
        RoomRankDto.create().roomId(channel2Id).rank(2),
        RoomRankDto.create().roomId(UUID.randomUUID()).rank(3))), user1Token);
      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if it has duplicated channels identifiers "
      + "then return a status code 400")
    public void updateChannelsRank_testRankListHasDuplicatedWorkspaceId() throws Exception {
      UUID channel1Id = UUID.fromString("51c874de-c262-4261-92dc-719f50a7f750");
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(channel1Id).rank(1),
        RoomRankDto.create().roomId(channel1Id).rank(2),
        RoomRankDto.create().roomId(UUID.randomUUID()).rank(3))), user1Token);
      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    @DisplayName("Given a pair list of room id and rank, if its ranks are not progressive number sequence "
      + "then return a status code 400")
    public void updateChannelsRank_testRankListNotProgressiveNumberSequence() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), objectMapper.writeValueAsString(List.of(
        RoomRankDto.create().roomId(UUID.randomUUID()).rank(1),
        RoomRankDto.create().roomId(UUID.randomUUID()).rank(3),
        RoomRankDto.create().roomId(UUID.randomUUID()).rank(5))), user1Token);
      assertEquals(400, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());
    }
  }

  @Nested
  @DisplayName("Get meeting by room id tests")
  public class GetMeetingByRoomIdTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/meeting", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the room meeting information with participants")
    public void getMeetingByRoomId_testOk() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
        ParticipantBuilder.create(user1Id, "user1session1").audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, "user2session1").audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(200, response.getStatus());
      MeetingDto meetingDto = objectMapper.readValue(response.getContentAsString(), MeetingDto.class);
      assertNotNull(meetingDto);
      assertEquals(meetingId, meetingDto.getId());
      assertEquals(roomId, meetingDto.getRoomId());
      assertNotNull(meetingDto.getParticipants());
      assertEquals(4, meetingDto.getParticipants().size());
      assertEquals(1, (int) meetingDto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).count());
      assertEquals(2, (int) meetingDto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meetingDto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      Optional<ParticipantDto> participant1 = meetingDto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertEquals("user1session1", participant1.get().getSessionId());
      assertTrue(participant1.get().isVideoStreamOn());
      assertTrue(participant1.get().isAudioStreamOn());
    }

    @Test
    @DisplayName("Given a room identifier, if the associated meeting doesn't exist then it returns a status code 404")
    public void getMeetingByRoomId_testMeetingNotExists() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void getMeetingByRoomId_testUserIsNotRoomMember() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exist then it returns a status code 404")
    public void getMeetingByRoomId_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the user isn’t authenticated then it returns a status code 401")
    public void getMeetingByRoomId_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

  }

  @Nested
  @DisplayName("Join room meeting tests")
  public class JoinRoomMeetingTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/meeting/join", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, if the room meeting exists the authenticated user correctly joins the meeting")
    public void joinRoomMeeting_testOkMeetingExists() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
        ParticipantBuilder.create(user2Id, "user2session1").audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").audioStreamOn(false).videoStreamOn(false)));
      Instant executionInstant = Instant.parse("2022-01-01T00:00:00Z");
      clock.fixTimeAt(executionInstant);
      MockHttpResponse response = dispatcher.put(url(roomId),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", "user1session1"), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(roomId.toString(), meeting.getRoomId());
      assertEquals(4, meeting.getParticipants().size());
      Participant newParticipant = meeting.getParticipants().stream().filter(participant ->
        user1Id.toString().equals(participant.getUserId()) && "user1session1".equals(participant.getSessionId())
      ).findAny().orElseThrow();
      assertTrue(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room meeting doesn't exist " +
      "it creates a new meeting associated with the indicated room and authenticated user correctly joins the meeting")
    public void joinRoomMeeting_testOkMeetingNotExists() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      Instant executionInstant = Instant.parse("2022-01-01T00:00:00Z");
      clock.fixTimeAt(executionInstant);
      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(meetingId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(meetingId.toString())).thenReturn(meetingId);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        response = dispatcher.put(url(roomId),
          objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
          Map.of("session-id", "user1session1"), user1Token);
      }
      clock.removeFixTime();
      assertEquals(200, response.getStatus());
      MeetingDto meetingDto = objectMapper.readValue(response.getContentAsString(), MeetingDto.class);
      assertNotNull(meetingDto);
      assertEquals("86cc37de-1217-4056-8c95-69997a6bccce", meetingDto.getId().toString());
      assertEquals(roomId, meetingDto.getRoomId());
      assertEquals(executionInstant, meetingDto.getCreatedAt().toInstant());
      assertNotNull(meetingDto.getParticipants());
      assertEquals(1, meetingDto.getParticipants().size());
      assertEquals(user1Id, meetingDto.getParticipants().get(0).getUserId());
      assertTrue(meetingDto.getParticipants().get(0).isAudioStreamOn());
      assertFalse(meetingDto.getParticipants().get(0).isVideoStreamOn());
      assertEquals("86cc37de-1217-4056-8c95-69997a6bccce",
        integrationTestUtils.getRoomById(roomId).orElseThrow().getMeetingId());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exists then it returns a status code 404")
    public void joinRoomMeeting_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", "user1session1"), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then return a status code 403")
    public void joinRoomMeeting_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.put(url(roomId),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", "user1session1"), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user isn’t authenticated then it returns a status code 401")
    public void joinRoomMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }
}
