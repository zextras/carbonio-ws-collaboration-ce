// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
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
import com.zextras.carbonio.chats.it.tools.VideoServerMockServer;
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
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import com.zextras.carbonio.chats.model.IdDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
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

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MeetingTestUtils meetingTestUtils;
  private final UserManagementMockServer userManagementMockServer;
  private final MongooseImMockServer mongooseImMockServer;
  private final StorageMockServer storageMockServer;
  private final RoomRepository roomRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final VideoServerMockServer videoServerMockServer;
  private final AppClock clock;

  public RoomsApiIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      MeetingTestUtils meetingTestUtils,
      UserManagementMockServer userManagementMockServer,
      MongooseImMockServer mongooseImMockServer,
      StorageMockServer storageMockServer,
      FileMetadataRepository fileMetadataRepository,
      Clock clock,
      RoomRepository roomRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      VideoServerMockServer videoServerMockServer) {
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
    this.videoServerMockServer = videoServerMockServer;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.clock = (AppClock) clock;
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static UUID user3Id;
  private static UUID user4Id;
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
      Optional.ofNullable(queryParams)
          .ifPresent(
              params ->
                  params.forEach(
                      (key, values) ->
                          values.forEach(
                              v -> {
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
      integrationTestUtils.generateAndSaveRoom(
          group1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(
          group2Id,
          RoomTypeDto.GROUP,
          "room2",
          List.of(user1Id, user2Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      MockHttpResponse response = dispatcher.get(url(null), user1Token);
      assertEquals(200, response.getStatus());
      assertFalse(response.getContentAsString().contains("members"));
      assertFalse(response.getContentAsString().contains("userSettings"));
      List<RoomDto> rooms =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, rooms.size());
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(group1Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(group2Id)));
      assertEquals(0, rooms.get(0).getMembers().size());
      assertEquals(0, rooms.get(1).getMembers().size());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());
      assertTrue(
          rooms.stream()
              .anyMatch(
                  room ->
                      room.getPictureUpdatedAt() != null
                          && room.getPictureUpdatedAt()
                              .equals(OffsetDateTime.parse("2022-01-01T00:00:00Z"))));
      assertTrue(rooms.stream().anyMatch(room -> room.getPictureUpdatedAt() == null));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly gets the rooms list with members of authenticated user")
    public void listRoom_testOkWithMembers() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(
          room2Id,
          RoomTypeDto.GROUP,
          "room2",
          List.of(user1Id, user2Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      MockHttpResponse response =
          dispatcher.get(url(Map.of("extraFields", List.of("members"))), user1Token);
      assertEquals(200, response.getStatus());
      assertFalse(response.getContentAsString().contains("userSettings"));
      List<RoomDto> rooms =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, rooms.size());
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(room1Id)));
      assertTrue(rooms.stream().anyMatch(r -> r.getId().equals(room2Id)));
      assertNotNull(rooms.get(0).getMembers());
      assertNotNull(rooms.get(1).getMembers());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());
      assertTrue(
          rooms.stream()
              .anyMatch(
                  room ->
                      room.getPictureUpdatedAt() != null
                          && room.getPictureUpdatedAt()
                              .equals(OffsetDateTime.parse("2022-01-01T00:00:00Z"))));
      assertTrue(rooms.stream().anyMatch(room -> room.getPictureUpdatedAt() == null));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly gets the rooms list with user settings of authenticated user")
    public void listRoom_testOkWithUserSettings() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(
          room2Id,
          RoomTypeDto.GROUP,
          "room2",
          List.of(user1Id, user2Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      MockHttpResponse response =
          dispatcher.get(url(Map.of("extraFields", List.of("settings"))), user1Token);
      assertEquals(200, response.getStatus());
      assertFalse(response.getContentAsString().contains("members"));
      List<RoomDto> rooms =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, rooms.size());

      RoomDto room1 =
          rooms.stream().filter(room -> room.getId().equals(room1Id)).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room1.getType());
      assertEquals(0, room1.getMembers().size());
      assertNotNull(room1.getUserSettings());
      assertNull(room1.getPictureUpdatedAt());

      RoomDto room2 =
          rooms.stream().filter(room -> room.getId().equals(room2Id)).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room2.getType());
      assertEquals(0, room2.getMembers().size());
      assertNotNull(room2.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room2.getPictureUpdatedAt());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Correctly gets the complete rooms list of authenticated user")
    public void listRoom_testOkCompleteRooms() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveRoom(
          room2Id,
          RoomTypeDto.GROUP,
          "room2",
          List.of(user1Id, user2Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      MockHttpResponse response =
          dispatcher.get(url(Map.of("extraFields", List.of("members", "settings"))), user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, rooms.size());

      RoomDto room1 =
          rooms.stream().filter(room -> room1Id.equals(room.getId())).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room1.getType());
      assertNotNull(room1.getMembers());
      assertNotNull(room1.getUserSettings());
      assertNull(room1.getPictureUpdatedAt());

      RoomDto room2 =
          rooms.stream().filter(room -> room2Id.equals(room.getId())).findAny().orElseThrow();
      assertEquals(RoomTypeDto.GROUP, room2.getType());
      assertNotNull(room2.getMembers());
      assertNotNull(room2.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room2.getPictureUpdatedAt());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Correctly returns an empty list if the authenticated user isn't a member for any room")
    public void listRoom_testNoRooms() throws Exception {
      MockHttpResponse response = dispatcher.get(url(null), user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
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
        mongooseImMockServer.mockCreateRoom(roomId.toString(), user1Id.toString(), true);
        mongooseImMockServer.mockAddRoomMember(
            roomId.toString(), user1Id.toString(), user2Id.toString(), true);
        String hopedXmppAffiliationMessage1 =
            String.format(
                    "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                        + " type='groupchat'>",
                    user1Id, roomId)
                + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
                + "<operation>memberAdded</operation>"
                + String.format("<user-id>%s</user-id>", user2Id)
                + "</x>"
                + "<body/>"
                + "</message>";
        mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage1, true);
        mongooseImMockServer.mockAddRoomMember(
            roomId.toString(), user1Id.toString(), user3Id.toString(), true);
        String hopedXmppAffiliationMessage2 =
            String.format(
                    "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                        + " type='groupchat'>",
                    user1Id, roomId)
                + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
                + "<operation>memberAdded</operation>"
                + String.format("<user-id>%s</user-id>", user3Id)
                + "</x>"
                + "<body/>"
                + "</message>";
        mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage2, true);
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      "testRoom", "Test room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
                  user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify(
            "GET", String.format("/users/id/%s", user2Id), user1Token, 1);
        userManagementMockServer.verify(
            "GET", String.format("/users/id/%s", user3Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.GROUP, room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(
            room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
            room.getMembers().stream()
                .filter(member -> user1Id.equals(member.getUserId()))
                .findAny()
                .orElseThrow()
                .isOwner());
        assertTrue(
            room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertTrue(
            room.getMembers().stream().anyMatch(member -> user3Id.equals(member.getUserId())));
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());

        mongooseImMockServer.verify(
            mongooseImMockServer.getCreateRoomRequest(roomId.toString(), user1Id.toString()),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getAddRoomMemberRequest(
                roomId.toString(), user1Id.toString(), user2Id.toString()),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage1),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getAddRoomMemberRequest(
                roomId.toString(), user1Id.toString(), user3Id.toString()),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage2),
            VerificationTimes.exactly(1));
        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given creation fields, if the name is not specified returns a status code 400")
      public void insertGroupRoom_testErrorWithoutName() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null, "Test room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getOutput().length);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

      @Test
      @DisplayName(
          "Given creation fields, if there aren't at least two member invitations returns a status"
              + " code 400")
      public void insertGroupRoom_testErrorRequestWithLessThanTwoMemberInvitations()
          throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    "testRoom", "Test room", RoomTypeDto.GROUP, List.of(user2Id)),
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
        mongooseImMockServer.mockCreateRoom(roomId.toString(), user1Id.toString(), true);
        mongooseImMockServer.mockAddRoomMember(
            roomId.toString(), user1Id.toString(), user2Id.toString(), true);
        String hopedXmppAffiliationMessage =
            String.format(
                    "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                        + " type='groupchat'>",
                    user1Id, roomId)
                + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
                + "<operation>memberAdded</operation>"
                + String.format("<user-id>%s</user-id>", user2Id)
                + "</x>"
                + "<body/>"
                + "</message>";
        mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);
        mongooseImMockServer.mockAddUserToContacts(user2Id.toString(), user1Id.toString(), true);
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(null, null, RoomTypeDto.ONE_TO_ONE, List.of(user2Id)),
                  user1Token);
        }
        clock.removeFixTime();
        userManagementMockServer.verify(
            "GET", String.format("/users/id/%s", user2Id), user1Token, 1);
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertNull(room.getName());
        assertNull(room.getDescription());
        assertEquals(RoomTypeDto.ONE_TO_ONE, room.getType());
        assertEquals(2, room.getMembers().size());
        assertTrue(
            room.getMembers().stream().anyMatch(member -> user1Id.equals(member.getUserId())));
        assertTrue(
            room.getMembers().stream()
                .filter(member -> user1Id.equals(member.getUserId()))
                .findAny()
                .orElseThrow()
                .isOwner());
        assertTrue(
            room.getMembers().stream().anyMatch(member -> user2Id.equals(member.getUserId())));
        assertEquals(executionInstant, room.getCreatedAt().toInstant());
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());

        mongooseImMockServer.verify(
            mongooseImMockServer.getCreateRoomRequest(roomId.toString(), user1Id.toString()),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getAddRoomMemberRequest(
                roomId.toString(), user1Id.toString(), user2Id.toString()),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
            VerificationTimes.exactly(1));
        mongooseImMockServer.verify(
            mongooseImMockServer.getAddUserToContactsRequest(
                user2Id.toString(), user1Id.toString()),
            VerificationTimes.exactly(1));
        // TODO: 23/02/22 verify event dispatcher interactions
      }

      @Test
      @DisplayName("Given one-to-one creation fields, if name is not null return a status code 400")
      public void insertOneToOneRoom_testErrorWithName() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    "testOneToOne", null, RoomTypeDto.ONE_TO_ONE, List.of(user2Id, user3Id)),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
        mongooseImMockServer.verifyZeroInteractions();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

      @Test
      @DisplayName(
          "Given one-to-one creation fields, if description is not null return a status code 400")
      public void insertOneToOneRoom_testErrorWithDescription() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null, "Test room", RoomTypeDto.ONE_TO_ONE, List.of(user2Id, user3Id)),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
        mongooseImMockServer.verifyZeroInteractions();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

      @Test
      @DisplayName(
          "Given creation fields for a one to one room, if there is a room with those users returns"
              + " a status code 409")
      public void insertOneToOneRoom_testAlreadyExists() throws Exception {
        UUID roomId = UUID.randomUUID();
        integrationTestUtils.generateAndSaveRoom(
            roomId, RoomTypeDto.ONE_TO_ONE, null, List.of(user1Id, user2Id));
        integrationTestUtils.generateAndSaveRoom(
            UUID.randomUUID(), RoomTypeDto.ONE_TO_ONE, null, List.of(user1Id, user3Id));
        integrationTestUtils.generateAndSaveRoom(
            UUID.randomUUID(), RoomTypeDto.ONE_TO_ONE, null, List.of(user2Id, user3Id));

        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(null, null, RoomTypeDto.ONE_TO_ONE, List.of(user2Id)),
                user1Token);
        assertEquals(409, response.getStatus());
        assertEquals(0, response.getOutput().length);
        mongooseImMockServer.verifyZeroInteractions();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }

      @Test
      @DisplayName(
          "Given one-to-one creation fields, if there are more then one invitation return a ststus"
              + " code 400")
      public void insertOneToOneRoom_testMoreThenOneInvitation() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null, null, RoomTypeDto.ONE_TO_ONE, List.of(user2Id, user3Id)),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
        mongooseImMockServer.verifyZeroInteractions();
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      }
    }

    @Test
    @DisplayName("Given creation fields, if there isn't name field returns a status code 400")
    public void insertRoom_testErrorRequestWithoutName() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  null, "Test room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given creation fields, if there isn't an authenticated user returns a status code 401")
    public void insertRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "room", "Room", RoomTypeDto.GROUP, List.of(user2Id, user3Id)),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given creation fields, if there are duplicated members returns a status code 400")
    public void insertRoom_testErrorDuplicatedMembers() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "room", "Room", RoomTypeDto.GROUP, List.of(user2Id, user2Id, user3Id)),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given creation fields, if there is current user into invites list returns a status code"
            + " 400")
    public void insertRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser()
        throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "room", "Room", RoomTypeDto.GROUP, List.of(user1Id, user2Id, user3Id)),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given creation fields, if there is a unknown member returns a status code 404")
    public void insertRoom_testErrorUnknownMember() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "testRoom",
                  "Test room",
                  RoomTypeDto.GROUP,
                  List.of(user2Id, user3Id, UUID.randomUUID())),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    private String getInsertRoomRequestBody(
        @Nullable String name,
        @Nullable String description,
        @Nullable RoomTypeDto type,
        @Nullable List<UUID> membersIds) {
      StringBuilder stringBuilder = new StringBuilder();

      Optional.ofNullable(name)
          .ifPresent(n -> stringBuilder.append(String.format("\"name\": \"%s\",", n)));
      Optional.ofNullable(description)
          .ifPresent(d -> stringBuilder.append(String.format("\"description\": \"%s\",", d)));
      Optional.ofNullable(type)
          .ifPresent(t -> stringBuilder.append(String.format("\"type\": \"%s\",", t)));
      Optional.ofNullable(membersIds)
          .ifPresent(
              ids -> {
                stringBuilder.append("\"membersIds\": [");
                stringBuilder.append(
                    ids.stream()
                        .map(id -> String.format("\"%s\"", id))
                        .collect(Collectors.joining(",")));
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
    @DisplayName(
        "Given a group room identifier, correctly returns the room information with members and"
            + " user settings")
    public void getGroupRoom_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "testRoom",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          List.of(user1Id),
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
    @DisplayName(
        "Given a room without a picture, correctly returns the room information with members and"
            + " user settings")
    public void getRoom_testOkWithoutPicture() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "testRoom",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          List.of(user1Id),
          null);

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
    @DisplayName(
        "Given a room identifier, if the user is not authenticated return a status code 401")
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
    @DisplayName(
        "Given a room identifier, if authenticated user isn't a room member then return a status"
            + " code 403")
    public void getRoom_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

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
      Instant insertRoomInstant =
          executionInstant.minus(Duration.ofDays(1L)).truncatedTo(ChronoUnit.SECONDS);
      clock.fixTimeAt(insertRoomInstant);
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "testRoom",
          "Test room",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      String hopedXmppMessage1 =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>roomNameChanged</operation><value"
              + " encoded='UTF-8'>\\\\u0075\\\\u0070\\\\u0064\\\\u0061\\\\u0074\\\\u0065\\\\u0064\\\\u0052\\\\u006f\\\\u006f\\\\u006d</value>"
              + "</x><body/></message>";
      String hopedXmppMessage2 =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>roomDescriptionChanged</operation><value"
              + " encoded='UTF-8'>\\\\u0055\\\\u0070\\\\u0064\\\\u0061\\\\u0074\\\\u0065\\\\u0064\\\\u0020\\\\u0072\\\\u006f\\\\u006f\\\\u006d</value>"
              + "</x><body/></message>";
      mongooseImMockServer.mockSendStanza(hopedXmppMessage1, true);
      mongooseImMockServer.mockSendStanza(hopedXmppMessage2, true);
      clock.fixTimeAt(executionInstant);
      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user1Token);
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
          mongooseImMockServer.getSendStanzaRequest(hopedXmppMessage1),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppMessage2),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a room identifier and update only name, correctly updates the room")
    public void updateRoom_testOkWithNameAndWithoutDescription() throws Exception {
      UUID roomId = UUID.randomUUID();
      Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
      Instant insertRoomInstant =
          executionInstant.minus(Duration.ofDays(1L)).truncatedTo(ChronoUnit.SECONDS);
      clock.fixTimeAt(insertRoomInstant);
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "testRoom",
          "Test room",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));

      String hopedXmppMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>roomNameChanged</operation><value"
              + " encoded='UTF-8'>\\\\u0075\\\\u0070\\\\u0064\\\\u0061\\\\u0074\\\\u0065\\\\u0064\\\\u0052\\\\u006f\\\\u006f\\\\u006d</value>"
              + "</x><body/></message>";

      mongooseImMockServer.mockSendStanza(hopedXmppMessage, true);
      clock.fixTimeAt(executionInstant);
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody("updatedRoom", null), user1Token);
      clock.removeFixTime();
      assertEquals(200, response.getStatus());
      RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
      assertEquals("updatedRoom", room.getName());
      assertEquals("Test room", room.getDescription());
      assertEquals(insertRoomInstant, room.getCreatedAt().toInstant());
      assertEquals(executionInstant, room.getUpdatedAt().toInstant());
      assertEquals(Duration.ofDays(1L), Duration.between(room.getCreatedAt(), room.getUpdatedAt()));
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppMessage),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a room identifier and update only description, correctly updates the room")
    public void updateRoom_testOkWithoutNameAndWithDescription() throws Exception {
      UUID roomId = UUID.randomUUID();
      Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
      Instant insertRoomInstant =
          executionInstant.minus(Duration.ofDays(1L)).truncatedTo(ChronoUnit.SECONDS);
      clock.fixTimeAt(insertRoomInstant);
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "testRoom",
          "Test room",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      String hopedXmppMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>roomDescriptionChanged</operation><value"
              + " encoded='UTF-8'>\\\\u0055\\\\u0070\\\\u0064\\\\u0061\\\\u0074\\\\u0065\\\\u0064\\\\u0020\\\\u0072\\\\u006f\\\\u006f\\\\u006d</value>"
              + "</x><body/></message>";
      mongooseImMockServer.mockSendStanza(hopedXmppMessage, true);
      clock.fixTimeAt(executionInstant);
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody(null, "Updated room"), user1Token);
      clock.removeFixTime();
      assertEquals(200, response.getStatus());
      RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
      assertEquals("testRoom", room.getName());
      assertEquals("Updated room", room.getDescription());
      assertEquals(insertRoomInstant, room.getCreatedAt().toInstant());
      assertEquals(executionInstant, room.getUpdatedAt().toInstant());
      assertEquals(Duration.ofDays(1L), Duration.between(room.getCreatedAt(), room.getUpdatedAt()));
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppMessage),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a room identifier, if there isn't any room return a status code 404")
    public void updateRoom_testErrorUpdateRoomNotExistingRoom() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody(null, "Updated room"), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if it is a one to one return status code 400")
    public void updateRoom_testErrorUpdateRoom1to1() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, null, List.of(user1Id, user2Id));
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody(null, "Updated room"), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if it is a group and the name and the description are null return"
            + " status code 400")
    public void updateRoom_testErrorUpdateRoomWithoutNameAndDescription() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          null,
          null,
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          List.of(user1Id),
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody(null, null), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and update fields, if the user is not authenticated return a"
            + " status code 401")
    public void updateRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              getUpdateRoomRequestBody("updatedRoom", "Updated room"),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a fake room identifier returns a status code 404")
    public void updateRoom_testErrorFakeRoomIdentifier() throws Exception {
      UUID roomId = UUID.randomUUID();

      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and update fields, "
            + "if authenticated user isn't a room member then return a status code 403")
    public void updateRoom_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and update fields, "
            + "if authenticated user isn't a room owner then return a status code 403")
    public void updateRoom_testErrorUserIsNotARoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    private String getUpdateRoomRequestBody(@Nullable String name, @Nullable String description) {
      String strName =
          Optional.ofNullable(name).map(n -> String.format("\"name\": \"%s\"", n)).orElse(null);
      String strDescr =
          Optional.ofNullable(description)
              .map(d -> String.format("\"description\": \"%s\"", d))
              .orElse(null);

      if (strName != null && strDescr == null) {
        return String.format("{%s}", strName);
      } else if (strName == null && strDescr != null) {
        return String.format("{%s}", strDescr);
      } else {
        return String.format("{%s}", String.join(", ", strName, strDescr));
      }
    }
  }

  @Nested
  @DisplayName("Deletes room tests")
  public class DeleteRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, if the room is a 1to1 returns a status code 403")
    public void deleteRoom_testErrorRoomIsA1to1() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a group room identifier, correctly deletes the room")
    public void deleteRoom_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
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
      mongooseImMockServer.verify(
          mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly deletes the room and the associated meeting")
    public void deleteRoom_groupWithStoppedMeetingTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId,
              RoomTypeDto.GROUP,
              List.of(
                  RoomMemberField.create().id(user1Id).owner(true),
                  RoomMemberField.create().id(user2Id).muted(true),
                  RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId,
              List.of(
                  ParticipantBuilder.create(user1Id, "user3Queue")
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertTrue(meetingTestUtils.getParticipant(meetingId, "user3Queue").isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(
          mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly deletes the room and stop and delete the"
            + " associated meeting")
    public void deleteRoom_groupWithActiveMeetingTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId,
              RoomTypeDto.GROUP,
              List.of(
                  RoomMemberField.create().id(user1Id).owner(true),
                  RoomMemberField.create().id(user2Id).muted(true),
                  RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId,
              List.of(
                  ParticipantBuilder.create(user1Id, "user3Queue")
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertTrue(meetingTestUtils.getParticipant(meetingId, "user3Queue").isEmpty());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      mongooseImMockServer.verify(
          mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
          VerificationTimes.exactly(1));

      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/videoHandleId",
              "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/audioHandleId",
              "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/audioHandleId",
              "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/videoHandleId",
              "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId",
              "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
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
        integrationTestUtils.generateAndSaveRoom(
            roomId,
            RoomTypeDto.GROUP,
            List.of(
                RoomMemberField.create().id(user1Id).owner(true),
                RoomMemberField.create().id(user2Id).muted(true),
                RoomMemberField.create().id(user3Id)));
        fileMetadataRepository.save(
            FileMetadata.create()
                .id(file1Id)
                .type(FileMetadataType.ATTACHMENT)
                .name("-")
                .userId(user1Id.toString())
                .roomId(roomId.toString())
                .originalSize(0L)
                .mimeType("-"));
        fileMetadataRepository.save(
            FileMetadata.create()
                .id(file2Id)
                .type(FileMetadataType.ATTACHMENT)
                .name("-")
                .userId(user1Id.toString())
                .roomId(roomId.toString())
                .originalSize(0L)
                .mimeType("-"));
        storageMockServer.setBulkDeleteResponse(
            List.of(file1Id, file2Id), List.of(file1Id, file2Id));
        mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
        assertTrue(
            fileMetadataRepository
                .getIdsByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT)
                .isEmpty());

        storageMockServer.verify(
            storageMockServer.getBulkDeleteRequest(List.of(file1Id, file2Id)),
            VerificationTimes.exactly(1));
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        mongooseImMockServer.verify(
            mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
            VerificationTimes.exactly(1));
      }

      @Test
      @DisplayName("Deletes the room and only attachments that storage service has deleted")
      public void deleteGroupRoomWithAttachments_storageServiceNotDeletesAllFiles()
          throws Exception {
        UUID roomId = UUID.randomUUID();
        String file1Id = UUID.randomUUID().toString();
        String file2Id = UUID.randomUUID().toString();
        integrationTestUtils.generateAndSaveRoom(
            roomId,
            RoomTypeDto.GROUP,
            List.of(
                RoomMemberField.create().id(user1Id).owner(true),
                RoomMemberField.create().id(user2Id).muted(true),
                RoomMemberField.create().id(user3Id)));
        fileMetadataRepository.save(
            FileMetadata.create()
                .id(file1Id)
                .type(FileMetadataType.ATTACHMENT)
                .name("-")
                .userId(user1Id.toString())
                .roomId(roomId.toString())
                .originalSize(0L)
                .mimeType("-"));
        fileMetadataRepository.save(
            FileMetadata.create()
                .id(file2Id)
                .type(FileMetadataType.ATTACHMENT)
                .name("-")
                .userId(user1Id.toString())
                .roomId(roomId.toString())
                .originalSize(0L)
                .mimeType("-"));
        storageMockServer.setBulkDeleteResponse(List.of(file1Id, file2Id), List.of(file1Id));
        mongooseImMockServer.mockDeleteRoom(roomId.toString(), true);

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
        assertTrue(fileMetadataRepository.getById(file1Id).isEmpty());
        Optional<FileMetadata> snoopy = fileMetadataRepository.getById(file2Id);
        assertTrue(snoopy.isPresent());
        assertNull(snoopy.get().getRoomId());

        storageMockServer.verify(
            storageMockServer.getBulkDeleteRequest(List.of(file1Id, file2Id)),
            VerificationTimes.exactly(1));
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        mongooseImMockServer.verify(
            mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
            VerificationTimes.exactly(1));
      }

      @Test
      @DisplayName(
          "Deletes the room but no associated attachments files because storage service failed")
      public void deleteGroupRoomWithAttachments_storageServiceFailed() throws Exception {
        UUID roomId = UUID.randomUUID();
        String file1Id = UUID.randomUUID().toString();
        String file2Id = UUID.randomUUID().toString();
        integrationTestUtils.generateAndSaveRoom(
            roomId,
            RoomTypeDto.GROUP,
            List.of(
                RoomMemberField.create().id(user1Id).owner(true),
                RoomMemberField.create().id(user2Id).muted(true),
                RoomMemberField.create().id(user3Id)));
        fileMetadataRepository.save(
            FileMetadata.create()
                .id(file1Id)
                .type(FileMetadataType.ATTACHMENT)
                .name("-")
                .userId(user1Id.toString())
                .roomId(roomId.toString())
                .originalSize(0L)
                .mimeType("-"));
        fileMetadataRepository.save(
            FileMetadata.create()
                .id(file2Id)
                .type(FileMetadataType.ATTACHMENT)
                .name("-")
                .userId(user1Id.toString())
                .roomId(roomId.toString())
                .originalSize(0L)
                .mimeType("-"));
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
            storageMockServer.getBulkDeleteRequest(List.of(file1Id, file2Id)),
            VerificationTimes.exactly(1));
        userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
        mongooseImMockServer.verify(
            mongooseImMockServer.getDeleteRoomRequest(roomId.toString()),
            VerificationTimes.exactly(1));
      }
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
    @DisplayName(
        "Given a room identifier, "
            + "if authenticated user isn't a room owner then return a status code 403")
    public void deleteRoom_testErrorUserIsNotARoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(200, response.getStatus());

      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
          String.format("inline; filename=\"%s\"", fileMock.getName()),
          response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(
          fileMock.getMimeType(),
          response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("GET", "/download", fileMock.getId(), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is not authenticated returns status code 401")
    public void getRoomPicture_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is not a room member returns status code 403")
    public void getRoomPicture_testErrorAuthenticatedUserIsNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room hasn't the picture returns status code 404")
    public void getRoomPicture_testErrorRoomHasNoPicture() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the storage hasn't the picture file returns status code 424")
    public void getRoomPicture_testErrorStorageHasNoPictureFile() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(
          roomId, FileMetadataType.ROOM_AVATAR, user1Id, roomId);
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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>roomPictureUpdated</operation>"
              + String.format("<picture-id>%s</picture-id>", roomId)
              + "<picture-name"
              + " encoded='UTF-8'>\\\\u0073\\\\u006e\\\\u006f\\\\u006f\\\\u0070\\\\u0079\\\\u002e\\\\u006a\\\\u0070\\\\u0067</picture-name>"
              + "</x><body/></message>";
      mongooseImMockServer.mockSendStanza(hoped, true);
      Instant now = Instant.now();
      clock.fixTimeAt(now);
      MockHttpResponse response =
          dispatcher.put(
              url(roomId),
              fileMock.getId().getBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "fileName",
                  "\\u0073\\u006e\\u006f\\u006f\\u0070\\u0079\\u002e\\u006a\\u0070\\u0067",
                  "mimeType",
                  fileMock.getMimeType()),
              user1Token);
      mongooseImMockServer.verify(mongooseImMockServer.getSendStanzaRequest(hoped));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      // TODO: 01/03/22 verify event dispatcher iterations
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      Optional<Room> room = roomRepository.getById(roomId.toString());
      assertTrue(room.isPresent());
      assertEquals(
          OffsetDateTime.ofInstant(now, clock.getZone()).toEpochSecond(),
          room.get().getUpdatedAt().toEpochSecond());

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if user is not authenticated returns status"
            + " code 401")
    public void updateRoomPicture_testErrorUnauthenticatedUser() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.fromString(fileMock.getId())),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "fileName",
                  Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
                  "mimeType",
                  fileMock.getMimeType()),
              null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if X-Content-Disposition is missing returns"
            + " status code 400")
    public void updateRoomPicture_testErrorMissingXContentDisposition() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.fromString(fileMock.getId())),
              fileMock.getFileBytes(),
              Collections.singletonMap("Content-Type", "application/octet-stream"),
              user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if user is not a room member returns status"
            + " code 403")
    public void updateRoomPicture_testErrorUserIsNotRoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "fileName",
                  "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                  "mimeType",
                  fileMock.getMimeType()),
              user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if user is not a room owner returns status"
            + " code 403")
    public void updateRoomPicture_testErrorUserIsNotRoomOwner() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user3Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "fileName",
                  "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                  "mimeType",
                  fileMock.getMimeType()),
              user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if the room isn't a group returns status code"
            + " 400")
    public void updateRoomPicture_testErrorRoomIsNotRoomGroup() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "X-Content-Disposition",
                  String.format(
                      "fileName=%s;mimeType=%s",
                      Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
                      fileMock.getMimeType())),
              user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if the image is too large returns status code"
            + " 400")
    public void updateRoomPicture_testErrorImageTooLarge() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_LARGE_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "X-Content-Disposition",
                  String.format(
                      "fileName=%s;mimeType=%s",
                      Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
                      fileMock.getMimeType())),
              user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if the file isn't an image returns status code"
            + " 400")
    public void updateRoomPicture_testErrorFileIsNotImage() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_PDF);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "X-Content-Disposition",
                  String.format(
                      "fileName=%s;mimeType=%s",
                      Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
                      fileMock.getMimeType())),
              user1Token);
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
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "room1",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          null,
          OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>roomPictureDeleted</operation>"
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hoped, true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertTrue(integrationTestUtils.getFileMetadataById(fileMock.getUUID()).isEmpty());
      assertNull(integrationTestUtils.getRoomById(roomId).orElseThrow().getPictureUpdatedAt());

      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the room hasn't a picture, it returns a status code 404")
    public void deleteRoomPicture_fileNotFound() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(
          MUTED_TO_INFINITY.toInstant(), roomUserSettings.get().getMutedUntil().toInstant());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings exists")
    public void muteRoom_testOkUserSettingExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(RoomUserSettings.create(room, user3Id.toString()));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(
          MUTED_TO_INFINITY.toInstant(), roomUserSettings.get().getMutedUntil().toInstant());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Correctly does nothing if the user is already muted")
    public void muteRoom_testOkUserAlreadyMuted() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(
          RoomUserSettings.create(room, user3Id.toString()).mutedUntil(MUTED_TO_INFINITY));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertEquals(
          MUTED_TO_INFINITY.toInstant(), roomUserSettings.get().getMutedUntil().toInstant());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void muteRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertFalse(roomUserSettings.isPresent());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Unmute the current user in a specific room")
    public void unmuteRoom_testOkUserSettingExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(
          RoomUserSettings.create(room, user3Id.toString()).mutedUntil(MUTED_TO_INFINITY));
      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertNull(roomUserSettings.get().getMutedUntil());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Correctly does nothing if the user has already unmuted")
    public void unmuteRoom_testOkUserAlreadyUnmuted() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.setRoomUserSettings(RoomUserSettings.create(room, user3Id.toString()));
      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertTrue(roomUserSettings.isPresent());
      assertNull(roomUserSettings.get().getMutedUntil());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void unmuteRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(200, response.getStatus());
      assertNotNull(
          objectMapper
              .readValue(response.getContentAsString(), ClearedDateDto.class)
              .getClearedAt());

      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Sets the clear date to now when user settings exists")
    public void clearRoom_testOkUserSettingExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      OffsetDateTime prevDate = OffsetDateTime.parse("2022-01-01T00:00:00Z");
      integrationTestUtils.setRoomUserSettings(
          RoomUserSettings.create(room, user3Id.toString()).clearedAt(prevDate));
      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(200, response.getStatus());
      assertTrue(
          prevDate.isBefore(
              objectMapper
                  .readValue(response.getContentAsString(), ClearedDateDto.class)
                  .getClearedAt()));
      // TODO: 23/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void clearRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

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
  @DisplayName("Gets list of room members tests")
  public class GetsListOfRoomMembersTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/members", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the list of room members")
    public void listRoomMember_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(200, response.getStatus());
      List<MemberDto> members =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      mongooseImMockServer.mockAddRoomMember(
          roomId.toString(), user1Id.toString(), user4Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberAdded</operation>"
              + String.format("<user-id>%s</user-id>", user4Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);
      MemberToInsertDto requestMember =
          MemberToInsertDto.create().userId(user4Id).historyCleared(false);
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(requestMember), user1Token);

      assertEquals(201, response.getStatus());
      MemberInsertedDto member =
          objectMapper.readValue(response.getContentAsString(), MemberInsertedDto.class);
      assertEquals(requestMember.getUserId(), member.getUserId());
      assertNull(member.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(
              roomId.toString(), user1Id.toString(), user4Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room is one to one returns status code 400")
    public void insertRoomMember_oneToOneTest() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly insert the new room member clearing the history")
    public void insertRoomMember_historyClearedOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      mongooseImMockServer.mockAddRoomMember(
          roomId.toString(), user1Id.toString(), user4Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberAdded</operation>"
              + String.format("<user-id>%s</user-id>", user4Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);
      MemberToInsertDto requestMember =
          MemberToInsertDto.create().userId(user4Id).historyCleared(true);
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(requestMember), user1Token);

      assertEquals(201, response.getStatus());
      MemberInsertedDto member =
          objectMapper.readValue(response.getContentAsString(), MemberInsertedDto.class);
      assertEquals(requestMember.getUserId(), member.getUserId());
      assertNotNull(member.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user4Id);
      assertTrue(roomUserSettings.isPresent());
      assertNotNull(roomUserSettings.get().getClearedAt());

      mongooseImMockServer.verify(
          mongooseImMockServer.getAddRoomMemberRequest(
              roomId.toString(), user1Id.toString(), user4Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if there isn't an authenticated user returns status code 401")
    public void insertRoomMember_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(url(UUID.randomUUID()), getInsertRoomMemberRequestBody(user1Id), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exist returns status code 404")
    public void insertRoomMember_testRoomNotExist() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user1Id), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the authenticated user isn't a room owner returns status code"
            + " 403")
    public void insertRoomMember_testAuthenticateUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is already a room member returns status code 400")
    public void insertRoomMember_testUserAlreadyRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user3Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user isn't an account returns status code 404")
    public void insertRoomMember_testUserNotHasAccount() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response =
          dispatcher.post(
              url(roomId), getInsertRoomMemberRequestBody(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a group room identifier, if the request doesn't contain historyCleared returns"
            + " status code 500")
    public void insertRoomMember_testHistoryClearedNotInitialized() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user1Token);

      assertEquals(500, response.getStatus());
      assertTrue(response.getContentAsString().isEmpty());

      userManagementMockServer.verify("GET", String.format("/users/id/%s", user4Id), user1Token, 1);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    private String getInsertRoomMemberRequestBody(MemberToInsertDto member) {
      return getInsertRoomMemberRequestBody(
          member.getUserId(),
          member.isOwner(),
          member.isTemporary(),
          member.isExternal(),
          member.isHistoryCleared());
    }

    private String getInsertRoomMemberRequestBody(@Nullable UUID userId) {
      return getInsertRoomMemberRequestBody(userId, null, null, null, null);
    }

    private String getInsertRoomMemberRequestBody(
        @Nullable UUID userId,
        @Nullable Boolean owner,
        @Nullable Boolean temporary,
        @Nullable Boolean external,
        @Nullable Boolean historyCleared) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("{");
      Optional.ofNullable(userId)
          .ifPresent(u -> stringBuilder.append(String.format("\"userId\": \"%s\",", u)));
      Optional.ofNullable(owner)
          .ifPresent(o -> stringBuilder.append(String.format("\"owner\": %s,", o)));
      Optional.ofNullable(temporary)
          .ifPresent(t -> stringBuilder.append(String.format("\"temporary\": %s,", t)));
      Optional.ofNullable(external)
          .ifPresent(e -> stringBuilder.append(String.format("\"external\": %s, ", e)));
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
    @DisplayName(
        "Given a group room identifier and a member identifier, correctly remove the user from room"
            + " members")
    public void deleteRoomMember_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberRemoved</operation>"
              + String.format("<user-id>%s</user-id>", user2Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);

      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user2Id.toString()))
              .findAny()
              .isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
          mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, "
            + "correctly leaves the user from the from room members")
    public void deleteRoomMember_memberIsNotActiveMeetingParticipantTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      String user2Queue = UUID.randomUUID().toString();
      Room roomEntity =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId,
              List.of(
                  ParticipantBuilder.create(user1Id, "user1Queue"),
                  ParticipantBuilder.create(user2Id, user2Queue),
                  ParticipantBuilder.create(user3Id, "user3Queue")));
      integrationTestUtils.updateRoom(roomEntity.meetingId(meetingId.toString()));

      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberRemoved</operation>"
              + String.format("<user-id>%s</user-id>", user2Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user2Id.toString()))
              .findAny()
              .isEmpty());
      Optional<Meeting> meeting = meetingTestUtils.getMeetingById(meetingId);
      assertTrue(meeting.isPresent());
      assertEquals(2, meeting.get().getParticipants().size());
      assertTrue(
          meeting.get().getParticipants().stream()
              .noneMatch(participant -> user2Id.toString().equals(participant.getUserId())));

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
          mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, correctly leaves the user from the"
            + " associated meeting and removes the user from room members")
    public void deleteRoomMember_memberIsActiveMeetingParticipantTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      String user2Queue = UUID.randomUUID().toString();
      Room roomEntity =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId,
              List.of(
                  ParticipantBuilder.create(user1Id, "user1Queue"),
                  ParticipantBuilder.create(user2Id, user2Queue),
                  ParticipantBuilder.create(user3Id, "user3Queue")));
      integrationTestUtils.updateRoom(roomEntity.meetingId(meetingId.toString()));

      VideoServerMeeting videoServerMeeting =
          meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId");
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  videoServerMeeting,
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  null,
                  null)
              .audioHandleId("audioHandleId_" + user2Queue));
      /*videoServerMockServer.mockRequestedResponse("POST",
      "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
      "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
      "{\"janus\":\"ack\"}", true);*/
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue,
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberRemoved</operation>"
              + String.format("<user-id>%s</user-id>", user2Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user2Id.toString()))
              .findAny()
              .isEmpty());
      Optional<Meeting> meeting = meetingTestUtils.getMeetingById(meetingId);
      assertTrue(meeting.isPresent());
      assertEquals(2, meeting.get().getParticipants().size());
      assertTrue(
          meeting.get().getParticipants().stream()
              .noneMatch(participant -> user2Id.toString().equals(participant.getUserId())));

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
          mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

      /*videoServerMockServer.verify(
      videoServerMockServer.getRequest("POST",
        "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
      VerificationTimes.exactly(1));*/
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
              "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connection_" + user2Queue,
              "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, correctly leaves the user from the"
            + " associated meeting, removes the meeting because the user was the last and removes"
            + " the user from room members")
    public void deleteRoomMember_memberIsLastMeetingParticipantTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      String user2Queue = UUID.randomUUID().toString();
      Room roomEntity =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId, List.of(ParticipantBuilder.create(user2Id, user2Queue)), true, null);
      integrationTestUtils.updateRoom(roomEntity.meetingId(meetingId.toString()));

      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meetingTestUtils.insertVideoServerMeeting(
                      meetingId.toString(),
                      "connectionId",
                      "audioHandleId",
                      "videoHandleId",
                      "audioRoomId",
                      "videoRoomId"),
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  null,
                  null)
              .audioHandleId("audioHandleId_" + user2Queue));
      /*videoServerMockServer.mockRequestedResponse("POST",
      "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
      "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
      "{\"janus\":\"ack\"}", true);*/
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue,
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user2Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberRemoved</operation>"
              + String.format("<user-id>%s</user-id>", user2Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);

      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user2Id.toString()))
              .findAny()
              .isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
          mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user2Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      /*videoServerMockServer.verify(
      videoServerMockServer.getRequest("POST",
        "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
      VerificationTimes.exactly(1));*/
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
              "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connection_" + user2Queue,
              "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/videoHandleId",
              "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/audioHandleId",
              "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/audioHandleId",
              "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId/videoHandleId",
              "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
      videoServerMockServer.verify(
          videoServerMockServer.getRequest(
              "POST",
              "/janus/connectionId",
              "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if there isn't an authenticated user"
            + " returns status code 401")
    public void deleteRoomMember_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user1Id), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if room doesn't exist returns status code"
            + " 404")
    public void deleteRoomMember_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user1Id), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the authenticated user isn't a room"
            + " owner returns status code 403")
    public void deleteRoomMember_testErrorUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room is a one to one returns"
            + " status code 403")
    public void deleteRoomMember_testErrorRoomOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier equals to authenticated user,"
            + " correctly user remove itself")
    public void deleteRoomMember_userRemoveItselfTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      mongooseImMockServer.mockRemoveRoomMember(roomId.toString(), user3Id.toString(), true);
      String hopedXmppAffiliationMessage =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user3Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberRemoved</operation>"
              + String.format("<user-id>%s</user-id>", user3Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage, true);
      MockHttpResponse response = dispatcher.delete(url(roomId, user3Id), user3Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user3Id.toString()))
              .findAny()
              .isEmpty());

      // TODO: 25/02/22 verify event dispatcher
      mongooseImMockServer.verify(
          mongooseImMockServer.getRemoveRoomMemberRequest(roomId.toString(), user3Id.toString()),
          VerificationTimes.exactly(1));
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hopedXmppAffiliationMessage),
          VerificationTimes.exactly(1));
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
    @DisplayName(
        "Given a room identifier and a member identifier, correctly promotes the member to owner")
    public void updateToOwner_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user2Id.toString()))
              .findAny()
              .orElseThrow()
              .isOwner());

      // TODO: 25/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if there isn't an authenticated user"
            + " return status code 401")
    public void updateToOwner_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID(), user1Id), null, null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room doesn't exist return status"
            + " code 404")
    public void updateToOwner_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID(), user2Id), null, user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the authenticated user isn't a room"
            + " owner return status code 403")
    public void updateToOwner_testAuthenticateUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the member doesn't exist return status"
            + " code 403")
    public void updateToOwner_testErrorMemberNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user4Id), null, user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room is a one-to-one then it"
            + " returns status code 400")
    public void updateToOwner_testErrorRoomIsOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.ONE_TO_ONE,
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(true)));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user1Token);
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
    @DisplayName(
        "Given a room identifier and a member identifier, correctly demotes a owner to normal"
            + " member")
    public void deleteOwner_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "room",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id, user2Id, user3Id),
          List.of(),
          null);
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertFalse(
          room.get().getSubscriptions().stream()
              .filter(s -> s.getUserId().equals(user2Id.toString()))
              .findAny()
              .orElseThrow()
              .isOwner());

      // TODO: 28/02/22 verify event dispatcher interactions
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if there isn't an authenticated user"
            + " returns status code 401")
    public void deleteOwner_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user2Id), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if authenticated user isn't a room owner"
            + " returns status code 403")
    public void deleteOwner_testErrorAuthenticatedUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "room",
          List.of(user1Id, user2Id, user3Id),
          List.of(user2Id, user3Id),
          List.of(),
          null);
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if requested user isn't a room member"
            + " returns status code 403")
    public void deleteOwner_testErrorUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room is a one-to-one then it"
            + " returns status code 400")
    public void deleteOwner_testErrorRoomIsOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.ONE_TO_ONE,
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(true)));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
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
    @DisplayName(
        "Given a room identifier, correctly returns a single paged list of attachments info of the"
            + " required room")
    public void listRoomAttachmentInfo_testOkSinglePage() throws Exception {

      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("faec1132-567d-451c-a969-18ca9131bdfa"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("6a6a1f06-0947-4b5f-a6ac-7631426e3a62"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("991b5178-1108-459e-a017-4197647167ec"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      FileMetadata file4 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("5a3d8dd2-f431-4195-acc1-5108948c6d26"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.removeFixTime();

      MockHttpResponse response = dispatcher.get(url(room1Id), user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto attachments =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(4, attachments.getAttachments().size());
      assertEquals(
          List.of(file3.getId(), file4.getId(), file1.getId(), file2.getId()),
          attachments.getAttachments().stream()
              .map(attachment -> attachment.getId().toString())
              .collect(Collectors.toList()));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName(
        "Given a room identifier, correctly returns multiple paged lists of attachments info of the"
            + " required room")
    public void listRoomAttachmentInfo_testOkMultiplePages() throws Exception {

      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("faec1132-567d-451c-a969-18ca9131bdfa"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("6a6a1f06-0947-4b5f-a6ac-7631426e3a62"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("991b5178-1108-459e-a017-4197647167ec"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2021-07-05T00:00:00Z"));
      FileMetadata file4 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("5a3d8dd2-f431-4195-acc1-5108948c6d26"),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.removeFixTime();

      MockHttpResponse response1 =
          dispatcher.get(String.join("", url(room1Id), "?itemsNumber=2"), user1Token);
      assertEquals(200, response1.getStatus());
      AttachmentsPaginationDto attachmentsPage1 =
          objectMapper.readValue(response1.getContentAsString(), AttachmentsPaginationDto.class);
      assertEquals(2, attachmentsPage1.getAttachments().size());
      assertEquals(
          List.of(file3.getId(), file1.getId()),
          attachmentsPage1.getAttachments().stream()
              .map(attachment -> attachment.getId().toString())
              .collect(Collectors.toList()));
      assertNotNull(attachmentsPage1.getFilter());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

      MockHttpResponse response2 =
          dispatcher.get(
              String.join("", url(room1Id), "?itemsNumber=2&filter=", attachmentsPage1.getFilter()),
              user1Token);
      assertEquals(200, response2.getStatus());
      AttachmentsPaginationDto attachmentsPage2 =
          objectMapper.readValue(response2.getContentAsString(), AttachmentsPaginationDto.class);
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
    @DisplayName(
        "Given a room identifier, if the user is not authenticated return a status code 401")
    public void listRoomAttachmentInfo_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if authenticated user isn't a room member then return a status"
            + " code 403")
    public void listRoomAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

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
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>attachmentAdded</operation>"
              + "<attachment-id>"
              + fileMock.getId()
              + "</attachment-id><filename"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073\\\\u002e\\\\u006a\\\\u0070\\\\u0067</filename>"
              + "<mime-type>image/jpg</mime-type><size>33786</size></x><body/></message>";
      mongooseImMockServer.mockSendStanza(hoped, true);

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.post(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "Content-Type", "application/octet-stream",
                    "fileName",
                        "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                    "mimeType", fileMock.getMimeType(),
                    "messageId", ""),
                user1Token);
      }

      assertEquals(201, response.getStatus());
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      IdDto id = objectMapper.readValue(response.getContentAsString(), IdDto.class);

      assertTrue(
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .stream()
              .anyMatch(attach -> attach.getId().equals(id.getId().toString())));
      // TODO: 28/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, correctly inserts the attachment with a"
            + " description")
    public void insertAttachment_testOkWithDescription() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' id='the-xmpp-message-id'"
                      + " to='%s@muclight.carbonio' type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>attachmentAdded</operation>"
              + "<attachment-id>"
              + fileMock.getId()
              + "</attachment-id><filename"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073\\\\u002e\\\\u006a\\\\u0070\\\\u0067</filename>"
              + "<mime-type>image/jpg</mime-type><size>33786</size></x><body"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073</body>"
              + "</message>";

      mongooseImMockServer.mockSendStanza(hoped, true);
      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.post(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "Content-Type", "application/octet-stream",
                    "fileName",
                        "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                    "mimeType", fileMock.getMimeType(),
                    "description", "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073",
                    "messageId", "the-xmpp-message-id",
                    "replyId", ""),
                user1Token);
      }

      assertEquals(201, response.getStatus());
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      IdDto id = objectMapper.readValue(response.getContentAsString(), IdDto.class);

      assertTrue(
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .stream()
              .anyMatch(attach -> attach.getId().equals(id.getId().toString())));
      // TODO: 28/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, correctly reply to the attachment with a"
            + " description")
    public void insertAttachment_testOkWithReply() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' id='the-xmpp-message-id'"
                      + " to='%s@muclight.carbonio' type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>attachmentAdded</operation>"
              + String.format("<attachment-id>%s</attachment-id>", fileMock.getId())
              + "<filename"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073\\\\u002e\\\\u006a\\\\u0070\\\\u0067</filename>"
              + "<mime-type>image/jpg</mime-type><size>33786</size></x><body"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073</body>"
              + String.format(
                  "<reply xmlns='urn:xmpp:reply:0' id='message-id-to-reply'"
                      + " to='%s@muclight.carbonio'/>",
                  roomId)
              + "</message>";
      mongooseImMockServer.mockSendStanza(hoped, true);
      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.post(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "Content-Type", "application/octet-stream",
                    "fileName",
                        "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                    "mimeType", fileMock.getMimeType(),
                    "description", "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073",
                    "messageId", "the-xmpp-message-id",
                    "replyId", "message-id-to-reply"),
                user1Token);
      }

      assertEquals(201, response.getStatus());
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      IdDto id = objectMapper.readValue(response.getContentAsString(), IdDto.class);

      assertTrue(
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .stream()
              .anyMatch(attach -> attach.getId().equals(id.getId().toString())));
      // TODO: 28/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, correctly inserts the attachment with an area")
    void insertAttachment_testOkWithArea() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' id='the-xmpp-message-id'"
                      + " to='%s@muclight.carbonio' type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>attachmentAdded</operation>"
              + "<attachment-id>"
              + fileMock.getId()
              + "</attachment-id><filename"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073\\\\u002e\\\\u006a\\\\u0070\\\\u0067</filename>"
              + "<mime-type>image/jpg</mime-type><size>33786</size><area>15x20</area></x><body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hoped, true);

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.post(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "Content-Type", "application/octet-stream",
                    "fileName",
                        "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                    "mimeType", fileMock.getMimeType(),
                    "messageId", "the-xmpp-message-id",
                    "area", "15x20"),
                user1Token);
      }

      assertEquals(201, response.getStatus());
      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("PUT", "/upload", fileMock.getId(), 1);
      IdDto id = objectMapper.readValue(response.getContentAsString(), IdDto.class);

      assertTrue(
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .stream()
              .anyMatch(attach -> attach.getId().equals(id.getId().toString())));
      // TODO: 28/02/22 verify event dispatcher interactions
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, if the area has a wrong format return a status"
            + " code 400")
    void insertAttachment_testErrorWithAreaWrongFormat() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' id='the-xmpp-message-id'"
                      + " to='%s@muclight.carbonio' type='groupchat'>",
                  user1Id, roomId)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>attachmentAdded</operation>"
              + "<attachment-id>"
              + fileMock.getId()
              + "</attachment-id><filename"
              + " encoded='UTF-8'>\\\\u0070\\\\u0065\\\\u0061\\\\u006e\\\\u0075\\\\u0074\\\\u0073\\\\u002e\\\\u006a\\\\u0070\\\\u0067</filename>"
              + "<mime-type>image/jpg</mime-type><size>33786</size><area>wrong_format</area></x>"
              + "<body/></message>";
      mongooseImMockServer.mockSendStanza(hoped, true);

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.post(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "Content-Type", "application/octet-stream",
                    "fileName", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
                    "mimeType", fileMock.getMimeType(),
                    "messageId", "the-xmpp-message-id",
                    "area", "wrong_format"),
                user1Token);
      }

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, if there isn't an authenticated user returns a"
            + " status code 401")
    public void insertAttachment_testErrorUnauthenticatedUser() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "fileName",
                  "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                  "mimeType",
                  fileMock.getMimeType()),
              null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, if authenticated isn't a room member returns a"
            + " status code 403")
    public void insertAttachment_testErrorAuthenticatedUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);

      MockHttpResponse response =
          dispatcher.post(
              url(roomId),
              fileMock.getFileBytes(),
              Map.of(
                  "Content-Type",
                  "application/octet-stream",
                  "fileName",
                  "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
                  "mimeType",
                  fileMock.getMimeType()),
              user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }
  }

  @Nested
  @DisplayName("Get meeting by room id tests")
  public class GetMeetingByRoomIdTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/meeting", roomId);
    }

    @Test
    @DisplayName(
        "Given a room identifier, correctly returns the room meeting information with participants")
    public void getMeetingByRoomId_testOk() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      UUID user1Queue = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue.toString())
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, UUID.randomUUID().toString())
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, UUID.randomUUID().toString())
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(200, response.getStatus());
      MeetingDto meetingDto =
          objectMapper.readValue(response.getContentAsString(), MeetingDto.class);
      assertNotNull(meetingDto);
      assertEquals(meetingId, meetingDto.getId());
      assertEquals(roomId, meetingDto.getRoomId());
      assertNotNull(meetingDto.getParticipants());
      assertEquals(3, meetingDto.getParticipants().size());
      assertEquals(
          1,
          (int)
              meetingDto.getParticipants().stream()
                  .filter(p -> user1Id.equals(p.getUserId()))
                  .count());
      assertEquals(
          1,
          (int)
              meetingDto.getParticipants().stream()
                  .filter(p -> user2Id.equals(p.getUserId()))
                  .count());
      assertEquals(
          1,
          (int)
              meetingDto.getParticipants().stream()
                  .filter(p -> user3Id.equals(p.getUserId()))
                  .count());
      Optional<ParticipantDto> participant1 =
          meetingDto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant1.isPresent());
      assertEquals(user1Id, participant1.get().getUserId());
      assertEquals(user1Queue.toString(), participant1.get().getQueueId());
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the associated meeting doesn't exist then it returns a status"
            + " code 404")
    public void getMeetingByRoomId_testMeetingNotExists() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user doesn't have an associated room member then it"
            + " returns a status code 403")
    public void getMeetingByRoomId_testUserIsNotRoomMember() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the room doesn't exist then it returns a status code 404")
    public void getMeetingByRoomId_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user isn’t authenticated then it returns a status code"
            + " 401")
    public void getMeetingByRoomId_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Forward Messages tests")
  public class ForwardMessagesTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/forward", roomId);
    }

    @Test
    @DisplayName("Forwards a text message")
    public void forwardMessages_textMessage() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<body/><forwarded xmlns='urn:xmpp:forward:0' count='1'><delay"
              + " xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/><message from='sender-id'"
              + " to='recipient-id' type='groupchat'><body"
              + " encoded='UTF-8'>\\\\u0074\\\\u006f\\\\u0020\\\\u0066\\\\u006f\\\\u0072\\\\u0077\\\\u0061\\\\u0072\\\\u0064</body>"
              + "</message></forwarded></message>";

      mongooseImMockServer.mockSendStanza(hoped, true);

      String messageToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><body>to forward</body></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"));
      MockHttpResponse response =
          dispatcher.post(
              url(roomId), objectMapper.writeValueAsString(List.of(forwardMessageDto)), user1Token);
      assertNotNull(response);
      assertEquals(204, response.getStatus());

      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Forwards a message describing an attachment")
    public void forwardMessages_attachmentMessage() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      UUID attach1Id = UUID.randomUUID();
      UUID attach2Id = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("group1")
              .description("group one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room2Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("group2")
              .description("group two"),
          List.of(
              RoomMemberField.create().id(user1Id),
              RoomMemberField.create().id(user3Id).owner(true),
              RoomMemberField.create().id(user4Id)));
      fileMetadataRepository.save(
          FileMetadata.create()
              .id(attach1Id.toString())
              .name("filename")
              .originalSize(1024L)
              .mimeType("mimetype")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(room1Id.toString()));
      storageMockServer.mockCopyFile(attach1Id.toString(), attach2Id.toString(), true);
      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, room2Id)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>attachmentAdded</operation>"
              + String.format("<attachment-id>%s</attachment-id>", attach2Id)
              + "<filename"
              + " encoded='UTF-8'>\\\\u0066\\\\u0069\\\\u006c\\\\u0065\\\\u006e\\\\u0061\\\\u006d\\\\u0065</filename>"
              + "<mime-type>mimetype</mime-type><size>1024</size></x><body/><forwarded"
              + " xmlns='urn:xmpp:forward:0' count='1'><delay xmlns='urn:xmpp:delay'"
              + " stamp='2023-01-01T00:00:00Z'/>"
              + String.format(
                  "<message from='%s@carbonio' to='%s@muclight.carbonio' type='groupchat'>",
                  user2Id, room1Id)
              + "<body/>"
              + "</message>"
              + "</forwarded>"
              + "</message>";

      mongooseImMockServer.mockSendStanza(hoped, true);

      String messageToForward =
          String.format(
                  "<message xmlns=\"jabber:client\" from=\"%s@carbonio\""
                      + " to=\"%s@muclight.carbonio\" type=\"groupchat\">",
                  user2Id, room1Id)
              + "<x xmlns=\"urn:xmpp:muclight:0#configuration\">"
              + "<operation>attachmentAdded</operation>"
              + String.format("<attachment-id>%s</attachment-id>", attach1Id)
              + "<filename>filename</filename>"
              + "<mime-type>mimetype</mime-type>"
              + "<size>1024</size>"
              + "</x><body/></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"));

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attach2Id);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
        uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
        uuid.when(() -> UUID.fromString(user4Id.toString())).thenReturn(user4Id);
        uuid.when(() -> UUID.fromString(room1Id.toString())).thenReturn(room1Id);
        uuid.when(() -> UUID.fromString(room2Id.toString())).thenReturn(room2Id);
        uuid.when(() -> UUID.fromString(attach1Id.toString())).thenReturn(attach1Id);
        uuid.when(() -> UUID.fromString(attach2Id.toString())).thenReturn(attach2Id);
        response =
            dispatcher.post(
                url(room2Id),
                objectMapper.writeValueAsString(List.of(forwardMessageDto)),
                user1Token);
      }

      assertNotNull(response);
      assertEquals(204, response.getStatus());
      FileMetadata fileMetadata =
          fileMetadataRepository.getById(attach2Id.toString()).orElseThrow();
      assertEquals(room2Id.toString(), fileMetadata.getRoomId());
      assertEquals("filename", fileMetadata.getName());
      assertEquals("mimetype", fileMetadata.getMimeType());
      assertEquals(1024, fileMetadata.getOriginalSize());

      storageMockServer.verify(
          storageMockServer.getCopyFileRequest(attach1Id.toString(), attach2Id.toString()),
          VerificationTimes.exactly(1));

      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Forwards a text message with multiple lines")
    public void forwardMessages_textMessageWithMultipleLines() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<body/><forwarded xmlns='urn:xmpp:forward:0' count='1'><delay"
              + " xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/><message from='sender-id'"
              + " to='recipient-id' type='groupchat'><body"
              + " encoded='UTF-8'>\\\\u0061\\\\u000a\\\\u0065\\\\u000a\\\\u0069\\\\u000a\\\\u006f\\\\u000a\\\\u0075\\\\u000a\\\\u0079</body>"
              + "</message></forwarded></message>";

      mongooseImMockServer.mockSendStanza(hoped, true);

      String messageToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><body>a\n"
              + "e\n"
              + "i\n"
              + "o\n"
              + "u\n"
              + "y</body></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"));
      MockHttpResponse response =
          dispatcher.post(
              url(roomId), objectMapper.writeValueAsString(List.of(forwardMessageDto)), user1Token);
      assertNotNull(response);
      assertEquals(204, response.getStatus());

      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Forwards a text message with special characters")
    public void forwardMessages_textMessageWithSpecialCharacters() throws Exception {
      UUID roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));

      String hoped =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, roomId)
              + "<body/><forwarded xmlns='urn:xmpp:forward:0' count='1'><delay"
              + " xmlns='urn:xmpp:delay' stamp='2023-01-01T00:00:00Z'/><message from='sender-id'"
              + " to='recipient-id' type='groupchat'><body"
              + " encoded='UTF-8'>\\\\u00e0\\\\u00e8\\\\u00e9\\\\u00ec\\\\u00f2\\\\u00f9\\\\u0026</body>"
              + "</message></forwarded></message>";

      mongooseImMockServer.mockSendStanza(hoped, true);

      String messageToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><body>àèéìòù&</body></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"));
      MockHttpResponse response =
          dispatcher.post(
              url(roomId), objectMapper.writeValueAsString(List.of(forwardMessageDto)), user1Token);
      assertNotNull(response);
      assertEquals(204, response.getStatus());

      mongooseImMockServer.verify(
          mongooseImMockServer.getSendStanzaRequest(hoped), VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "If the authenticated user is not a member of the attachment room to forward, correctly"
            + " returns a status code 403")
    public void forwardMessages_userNotMemberOfAttachmentRoom() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      UUID attachId = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("group1")
              .description("group one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room2Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("group2")
              .description("group two"),
          List.of(
              RoomMemberField.create().id(user1Id),
              RoomMemberField.create().id(user3Id).owner(true),
              RoomMemberField.create().id(user4Id)));
      fileMetadataRepository.save(
          FileMetadata.create()
              .id(attachId.toString())
              .name("filename")
              .originalSize(1024L)
              .mimeType("mimetype")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(room1Id.toString()));
      String messageToForward =
          String.format(
                  "<message xmlns=\"jabber:client\" from=\"%s@carbonio\""
                      + " to=\"%s@muclight.carbonio\" type=\"groupchat\">",
                  user2Id, room1Id)
              + "<x xmlns=\"urn:xmpp:muclight:0#configuration\">"
              + "<operation>attachmentAdded</operation>"
              + String.format("<attachment-id>%s</attachment-id>", attachId)
              + "<filename>filename</filename>"
              + "<mime-type>mimetype</mime-type>"
              + "<size>1024</size>"
              + "</x><body/></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body !");

      MockHttpResponse response =
          dispatcher.post(
              url(room2Id),
              objectMapper.writeValueAsString(List.of(forwardMessageDto)),
              user3Token);

      assertNotNull(response);
      assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("If the attachment to forward not exists, correctly returns a status code 404")
    public void forwardMessages_attachmentNotFound() throws Exception {
      UUID roomId = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("group1")
              .description("group one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      String messageToForward =
          String.format(
                  "<message xmlns=\"jabber:client\" from=\"%s@carbonio\""
                      + " to=\"%s@muclight.carbonio\" type=\"groupchat\">",
                  user2Id, roomId)
              + "<x xmlns=\"urn:xmpp:muclight:0#configuration\">"
              + "<operation>attachmentAdded</operation>"
              + String.format("<attachment-id>%s</attachment-id>", UUID.randomUUID())
              + "<filename>filename</filename>"
              + "<mime-type>mimetype</mime-type>"
              + "<size>1024</size>"
              + "</x><body/></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body !");

      MockHttpResponse response =
          dispatcher.post(
              url(roomId), objectMapper.writeValueAsString(List.of(forwardMessageDto)), user1Token);

      assertNotNull(response);
      assertEquals(404, response.getStatus());
    }

    @Test
    @DisplayName(
        "If the authenticated user is not a member of destination room, correctly returns a status"
            + " code 403")
    public void forwardMessages_userNotMemberOfDestinationRoom() throws Exception {
      UUID roomId = UUID.randomUUID();

      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(roomId.toString())
              .type(RoomTypeDto.GROUP)
              .name("group")
              .description("group"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      String messageToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><body>this is the body of the message to forward!</body>"
              + "</message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body !");

      MockHttpResponse response =
          dispatcher.post(
              url(roomId), objectMapper.writeValueAsString(List.of(forwardMessageDto)), user3Token);

      assertNotNull(response);
      assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("If the current user is not authenticated, correctly returns a status code 401")
    public void forwardMessages_userNotAuthenticated() throws Exception {
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage("<>")
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body !");

      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(List.of(forwardMessageDto)),
              null);
      assertNotNull(response);
      assertEquals(401, response.getStatus());
    }
  }
}
