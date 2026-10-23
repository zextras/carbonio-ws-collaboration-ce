// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

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
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
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
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsRequestDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.ClearedDateDto;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.MessageDto;
import com.zextras.carbonio.chats.model.ParticipantDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.annotation.Nullable;
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
import org.apache.http.entity.ContentType;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ApiIntegrationTest
public class RoomsApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MeetingTestUtils meetingTestUtils;
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
  class GetsRoomsListTests {

    private String url(@Nullable Map<String, List<String>> queryParams) {
      StringBuilder url = new StringBuilder();
      Optional.ofNullable(queryParams)
          .ifPresent(
              params ->
                  params.forEach(
                      (key, values) ->
                          values.forEach(
                              v -> {
                                url.append(!url.isEmpty() ? "&" : "");
                                url.append(String.join("=", key, v));
                              })));
      return "/rooms" + (!url.isEmpty() ? "?" + url : "");
    }

    @Test
    @DisplayName("Correctly gets the basic rooms of authenticated user")
    void listRoom_testOkBasicRooms() throws Exception {
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
    }

    @Test
    @DisplayName("Correctly gets the rooms list with members of authenticated user")
    void listRoom_testOkWithMembers() throws Exception {
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
    }

    @Test
    @DisplayName("Correctly gets the rooms list with user settings of authenticated user")
    void listRoom_testOkWithUserSettings() throws Exception {
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
    }

    @Test
    @DisplayName("Correctly gets the complete rooms list of authenticated user")
    void listRoom_testOkCompleteRooms() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Correctly returns an empty list if the authenticated user isn't a member for any room")
    void listRoom_testNoRooms() throws Exception {
      MockHttpResponse response = dispatcher.get(url(null), user1Token);
      assertEquals(200, response.getStatus());
      List<RoomDto> rooms =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(0, rooms.size());
    }

    @Test
    @DisplayName("If there isn't an authenticated user return a status code 401")
    void listRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(null), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Insert room tests")
  class InsertRoomTests {

    private static final String URL = "/rooms";

    @Nested
    @DisplayName("Insert group room tests")
    class InsertGroupRoomTests {

      @Test
      @DisplayName("Given creation fields, inserts a new group room and returns its data")
      void insertGroupRoom_testOk() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        UUID[] eventIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId, eventIds[0], eventIds[1], eventIds[2]);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      "testRoom",
                      "Test room",
                      RoomTypeDto.GROUP,
                      List.of(
                          MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                  user1Token);
        }
        clock.removeFixTime();
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.GROUP, room.getType());
        assertEquals(3, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertFalse(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        assertFalse(room.getCreatedAt().toInstant().isBefore(executionInstant));
        assertFalse(room.getCreatedAt().toInstant().isAfter(Instant.now()));
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
      }

      @Test
      @DisplayName(
          "Given creation fields, inserts a new group room settings the owners and returns its"
              + " data")
      void insertGroupRoom_testOkWithOwners() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        UUID[] eventIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId, eventIds[0], eventIds[1], eventIds[2]);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      "testRoom",
                      "Test room",
                      RoomTypeDto.GROUP,
                      List.of(
                          MemberDto.create().userId(user2Id).owner(true),
                          MemberDto.create().userId(user3Id))),
                  user1Token);
        }
        clock.removeFixTime();
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.GROUP, room.getType());
        assertEquals(3, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertTrue(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        assertFalse(room.getCreatedAt().toInstant().isBefore(executionInstant));
        assertFalse(room.getCreatedAt().toInstant().isAfter(Instant.now()));
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
      }

      @Test
      @DisplayName("Given creation fields, if the name is not specified returns a status code 400")
      void insertGroupRoom_testErrorWithoutName() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null,
                    "Test room",
                    RoomTypeDto.GROUP,
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getOutput().length);
      }

      @Test
      @DisplayName(
          "Given creation fields, if there aren't at least two member invitations returns a status"
              + " code 400")
      void insertGroupRoom_testErrorRequestWithLessThanTwoMemberInvitations() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    "testRoom",
                    "Test room",
                    RoomTypeDto.GROUP,
                    List.of(MemberDto.create().userId(user2Id))),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getOutput().length);
      }
    }

    @Nested
    @DisplayName("Insert temporary room tests")
    class InsertTemporaryRoomTests {

      @Test
      @DisplayName("Given creation fields, inserts a new temporary room and returns its data")
      void insertTemporaryRoom_testOk() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        UUID[] eventIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId, eventIds[0], eventIds[1], eventIds[2]);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      "testRoom",
                      "Test room",
                      RoomTypeDto.TEMPORARY,
                      List.of(
                          MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                  user1Token);
        }
        clock.removeFixTime();
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.TEMPORARY, room.getType());
        assertEquals(3, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertFalse(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        assertFalse(room.getCreatedAt().toInstant().isBefore(executionInstant));
        assertFalse(room.getCreatedAt().toInstant().isAfter(Instant.now()));
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
      }

      @Test
      @DisplayName(
          "Given creation fields, inserts a new temporary room settings the owners and returns its"
              + " data")
      void insertTemporaryRoom_testOkWithOwners() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        UUID[] eventIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId, eventIds[0], eventIds[1], eventIds[2]);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      "testRoom",
                      "Test room",
                      RoomTypeDto.TEMPORARY,
                      List.of(
                          MemberDto.create().userId(user2Id).owner(true),
                          MemberDto.create().userId(user3Id))),
                  user1Token);
        }
        clock.removeFixTime();
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.TEMPORARY, room.getType());
        assertEquals(3, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertTrue(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        assertFalse(room.getCreatedAt().toInstant().isBefore(executionInstant));
        assertFalse(room.getCreatedAt().toInstant().isAfter(Instant.now()));
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
      }

      @Test
      @DisplayName("Given creation fields, if the name is not specified returns a status code 400")
      void insertTemporaryRoom_testErrorWithoutName() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null,
                    "Test room",
                    RoomTypeDto.TEMPORARY,
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getOutput().length);
      }

      @Test
      @DisplayName(
          "Given creation fields, create a room without adding more members but only creator as"
              + " owner")
      void insertTemporaryRoom_testOkWithOneMember() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      "testRoom", "Test room", RoomTypeDto.TEMPORARY, Collections.emptyList()),
                  user1Token);
        }
        clock.removeFixTime();
        assertEquals(201, response.getStatus());
        RoomDto room = objectMapper.readValue(response.getContentAsString(), RoomDto.class);
        assertEquals("testRoom", room.getName());
        assertEquals("Test room", room.getDescription());
        assertEquals(RoomTypeDto.TEMPORARY, room.getType());
        assertEquals(1, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());

        assertFalse(room.getCreatedAt().toInstant().isBefore(executionInstant));
        assertFalse(room.getCreatedAt().toInstant().isAfter(Instant.now()));
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
      }
    }

    @Nested
    @DisplayName("Insert one-to-one room tests")
    class InsertOneToOneRoomTests {

      @Test
      @DisplayName("Given creation fields, inserts a new one-to-one room and returns its data")
      void insertOneToOneRoom_testOk() throws Exception {
        Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        clock.fixTimeAt(executionInstant);
        MockHttpResponse response;
        UUID roomId = UUID.fromString("c9f83f1c-9b96-4731-9404-79e45a5d6d3c");
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomId);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
          response =
              dispatcher.post(
                  URL,
                  getInsertRoomRequestBody(
                      null,
                      null,
                      RoomTypeDto.ONE_TO_ONE,
                      List.of(MemberDto.create().userId(user2Id))),
                  user1Token);
        }
        clock.removeFixTime();
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
        assertFalse(room.getCreatedAt().toInstant().isBefore(executionInstant));
        assertFalse(room.getCreatedAt().toInstant().isAfter(Instant.now()));
        assertEquals(executionInstant, room.getUpdatedAt().toInstant());
        assertNull(room.getPictureUpdatedAt());
      }

      @Test
      @DisplayName("Given one-to-one creation fields, if name is not null return a status code 400")
      void insertOneToOneRoom_testErrorWithName() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    "testOneToOne",
                    null,
                    RoomTypeDto.ONE_TO_ONE,
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
      }

      @Test
      @DisplayName(
          "Given one-to-one creation fields, if description is not null return a status code 400")
      void insertOneToOneRoom_testErrorWithDescription() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null,
                    "Test room",
                    RoomTypeDto.ONE_TO_ONE,
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
      }

      @Test
      @DisplayName(
          "Given creation fields for a one-to-one room, if there is a room with those users returns"
              + " a status code 409")
      void insertOneToOneRoom_testAlreadyExists() throws Exception {
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
                getInsertRoomRequestBody(
                    null,
                    null,
                    RoomTypeDto.ONE_TO_ONE,
                    List.of(MemberDto.create().userId(user2Id))),
                user1Token);
        assertEquals(409, response.getStatus());
        assertEquals(0, response.getOutput().length);
      }

      @Test
      @DisplayName(
          "Given one-to-one creation fields, if there are more then one invitation return a ststus"
              + " code 400")
      void insertOneToOneRoom_testMoreThenOneInvitation() throws Exception {
        MockHttpResponse response =
            dispatcher.post(
                URL,
                getInsertRoomRequestBody(
                    null,
                    null,
                    RoomTypeDto.ONE_TO_ONE,
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
                user1Token);

        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsString().length());
      }
    }

    @Test
    @DisplayName("Given creation fields, if there isn't name field returns a status code 400")
    void insertRoom_testErrorRequestWithoutName() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  null,
                  "Test room",
                  RoomTypeDto.GROUP,
                  List.of(MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given creation fields, if there isn't an authenticated user returns a status code 401")
    void insertRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "room",
                  "Room",
                  RoomTypeDto.GROUP,
                  List.of(MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id))),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given creation fields, if there are duplicated members returns a status code 400")
    void insertRoom_testErrorDuplicatedMembers() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "room",
                  "Room",
                  RoomTypeDto.GROUP,
                  List.of(
                      MemberDto.create().userId(user2Id),
                      MemberDto.create().userId(user2Id),
                      MemberDto.create().userId(user3Id))),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given creation fields, if there is current user into invites list returns a status code"
            + " 400")
    void insertRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "room",
                  "Room",
                  RoomTypeDto.GROUP,
                  List.of(
                      MemberDto.create().userId(user1Id),
                      MemberDto.create().userId(user2Id),
                      MemberDto.create().userId(user3Id))),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given creation fields, if there is a unknown member returns a status code 404")
    void insertRoom_testErrorUnknownMember() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              getInsertRoomRequestBody(
                  "testRoom",
                  "Test room",
                  RoomTypeDto.GROUP,
                  List.of(
                      MemberDto.create().userId(user2Id),
                      MemberDto.create().userId(user3Id),
                      MemberDto.create().userId(UUID.randomUUID()))),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    private String getInsertRoomRequestBody(
        @Nullable String name,
        @Nullable String description,
        @Nullable RoomTypeDto type,
        @Nullable List<MemberDto> members) {
      StringBuilder stringBuilder = new StringBuilder();

      Optional.ofNullable(name)
          .ifPresent(n -> stringBuilder.append(String.format("\"name\": \"%s\",", n)));
      Optional.ofNullable(description)
          .ifPresent(d -> stringBuilder.append(String.format("\"description\": \"%s\",", d)));
      Optional.ofNullable(type)
          .ifPresent(t -> stringBuilder.append(String.format("\"type\": \"%s\",", t)));
      Optional.ofNullable(members)
          .ifPresent(
              memberList -> {
                stringBuilder.append("\"members\": [");
                stringBuilder.append(
                    memberList.stream()
                        .map(
                            member ->
                                String.format(
                                    "{\"userId\": \"%s\", \"owner\": \"%s\"}",
                                    member.getUserId(), member.isOwner()))
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
  class GetsRoomByIdentifierTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly returns the room information with members and"
            + " user settings")
    void getGroupRoom_testOk() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room without a picture, correctly returns the room information with members and"
            + " user settings")
    void getRoom_testOkWithoutPicture() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is not authenticated return a status code 401")
    void getRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a fake room identifier returns a status code 404")
    void getRoom_testErrorFakeRoomIdentifier() throws Exception {
      UUID roomId = UUID.randomUUID();

      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if authenticated user isn't a room member then return a status"
            + " code 403")
    void getRoom_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Updates room tests")
  class UpdatesRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a room identifier and update fields, correctly updates the room")
    void updateRoom_testOk() throws Exception {
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
    }

    @Test
    @DisplayName("Given a room identifier and update only name, correctly updates the room")
    void updateRoom_testOkWithNameAndWithoutDescription() throws Exception {
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
    }

    @Test
    @DisplayName("Given a room identifier and update only description, correctly updates the room")
    void updateRoom_testOkWithoutNameAndWithDescription() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and update name, correctly updates the room name and the"
            + " associated meeting name")
    void updateRoomWithMeeting_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      Instant executionInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
      Instant insertRoomInstant =
          executionInstant.minus(Duration.ofDays(1L)).truncatedTo(ChronoUnit.SECONDS);
      clock.fixTimeAt(insertRoomInstant);
      Room roomCreated =
          integrationTestUtils.generateAndSaveRoom(
              roomId,
              RoomTypeDto.GROUP,
              "testRoom",
              "Test room",
              List.of(user1Id, user2Id, user3Id),
              List.of(user1Id),
              List.of(user1Id),
              OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(roomId, List.of());
      integrationTestUtils.updateRoom(roomCreated.meetingId(meetingId.toString()));
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
      Optional<Meeting> meeting = meetingTestUtils.getMeetingById(meetingId);
      assertTrue(meeting.isPresent());
      Meeting updatedMeeting = meeting.get();
      assertEquals(roomId.toString(), updatedMeeting.getRoomId());
      assertEquals("updatedRoom", updatedMeeting.getName());
    }

    @Test
    @DisplayName("Given a room identifier, if there isn't any room return a status code 404")
    void updateRoom_testErrorUpdateRoomNotExistingRoom() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody(null, "Updated room"), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if it is a one-to-one return status code 400")
    void updateRoom_testErrorUpdateRoom1to1() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, null, List.of(user1Id, user2Id));
      MockHttpResponse response =
          dispatcher.put(url(roomId), getUpdateRoomRequestBody(null, "Updated room"), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if it is a group and the name and the description are null return"
            + " status code 400")
    void updateRoom_testErrorUpdateRoomWithoutNameAndDescription() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and update fields, if the user is not authenticated return a"
            + " status code 401")
    void updateRoom_testErrorUnauthenticatedUser() throws Exception {
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
    void updateRoom_testErrorFakeRoomIdentifier() throws Exception {
      UUID roomId = UUID.randomUUID();

      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and update fields, "
            + "if authenticated user isn't a room member then return a status code 403")
    void updateRoom_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and update fields, "
            + "if authenticated user isn't a room owner then return a status code 403")
    void updateRoom_testErrorUserIsNotARoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response =
          dispatcher.put(
              url(roomId), getUpdateRoomRequestBody("updatedRoom", "Updated room"), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
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
  class DeleteRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, if the room is a 1to1 returns a status code 403")
    void deleteRoom_testErrorRoomIsA1to1() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a group room identifier, correctly deletes the room")
    void deleteRoom_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).muted(true),
              RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly deletes the room and the associated meeting")
    void deleteRoom_groupWithStoppedMeetingTestOk() throws Exception {
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

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
      assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertTrue(meetingTestUtils.getParticipant(meetingId, "user3Queue").isEmpty());
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly deletes the room and stop and delete the"
            + " associated meeting")
    void deleteRoom_groupWithActiveMeetingTestOk() throws Exception {
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
    }

    @Nested
    @DisplayName("Delete group room with attachments tests")
    class DeleteGroupRoomWithAttachmentsTests {

      @Test
      @DisplayName("Correctly deletes the room and all associated attachments files")
      void deleteGroupRoomWithAttachments_testOk() throws Exception {
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
        // storages bulkDelete returns the FAILED (not-deleted) ids: empty => all deleted
        storageMockServer.setBulkDeleteResponse(List.of(file1Id, file2Id), List.of());

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
        assertTrue(
            fileMetadataRepository
                .getIdsByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT)
                .isEmpty());
        assertTrue(fileMetadataRepository.getById(file1Id).isEmpty());
        assertTrue(fileMetadataRepository.getById(file2Id).isEmpty());
      }

      @Test
      @DisplayName("Deletes the room and only attachments that storage service has deleted")
      void deleteGroupRoomWithAttachments_storageServiceNotDeletesAllFiles() throws Exception {
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
        // storages bulkDelete returns the FAILED (not-deleted) ids: file2 failed => file1 deleted
        storageMockServer.setBulkDeleteResponse(List.of(file1Id, file2Id), List.of(file2Id));

        MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);

        assertEquals(204, response.getStatus());
        assertEquals(0, response.getOutput().length);
        assertTrue(integrationTestUtils.getRoomById(roomId).isEmpty());
        assertTrue(roomUserSettingsRepository.getByRoomId(roomId.toString()).isEmpty());
        assertTrue(fileMetadataRepository.getById(file1Id).isEmpty());
        Optional<FileMetadata> snoopy = fileMetadataRepository.getById(file2Id);
        assertTrue(snoopy.isPresent());
        assertNull(snoopy.get().getRoomId());
      }

      @Test
      @DisplayName(
          "Deletes the room but no associated attachments files because storage service failed")
      void deleteGroupRoomWithAttachments_storageServiceFailed() throws Exception {
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
      }
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not authenticated return status code 401")
    void deleteRoom_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room not exists returns a status code 404")
    void deleteRoom_testErrorRoomNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, "
            + "if authenticated user isn't a room owner then return a status code 403")
    void deleteRoom_testErrorUserIsNotARoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Gets room picture tests")
  class GetsRoomPictureTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/picture", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the room picture")
    void getRoomPicture_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);
      storageMockServer.mockDownload(fileMock, true);

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
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is not authenticated returns status code 401")
    void getRoomPicture_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is not a room member returns status code 403")
    void getRoomPicture_testErrorAuthenticatedUserIsNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room hasn't the picture returns status code 404")
    void getRoomPicture_testErrorRoomHasNoPicture() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the storage hasn't the picture file returns status code 424")
    void getRoomPicture_testErrorStorageHasNoPictureFile() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      integrationTestUtils.generateAndSaveFileMetadata(
          roomId, "Room image", "image/png", FileMetadataType.ROOM_AVATAR, user1Id, roomId);
      MockHttpResponse response = dispatcher.get(url(roomId), user1Token);
      assertEquals(424, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Update room picture tests")
  class UpdateRoomPictureTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/picture", roomId);
    }

    @Test
    @DisplayName("Given a room identifier and a picture file, correctly updates the room pictures")
    void updateRoomPicture_testOk() throws Exception {
      FileMock existingImage = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      FileMock newImage = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "room1",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          null,
          OffsetDateTime.parse("2022-01-01T13:00:00Z"));
      integrationTestUtils.generateAndSaveFileMetadata(
          existingImage, FileMetadataType.ROOM_AVATAR, user1Id, roomId);
      storageMockServer.mockDelete(existingImage.getId(), true);
      storageMockServer.mockUpload(
          newImage,
          new StorageMockServer.UploadResponse()
              .digest("")
              .digestAlgorithm("")
              .size(newImage.getSize()),
          true);
      Instant now = Instant.now();
      clock.fixTimeAt(now);
      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(newImage.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.put(
                url(roomId),
                newImage.getFileBytes(),
                Map.of(
                    "Content-Type",
                    "application/octet-stream",
                    "fileName",
                    StringFormatUtils.encodeToUtf8("snoopy.jpg"),
                    "mimeType",
                    newImage.getMimeType(),
                    "Content-Length",
                    String.valueOf(newImage.getSize())),
                user1Token);
      }
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Optional<Room> room = roomRepository.getById(roomId.toString());
      assertTrue(room.isPresent());
      assertEquals(
          OffsetDateTime.ofInstant(now, clock.getZone()).toEpochSecond(),
          room.get().getUpdatedAt().toEpochSecond());
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if user is not authenticated returns status"
            + " code 401")
    void updateRoomPicture_testErrorUnauthenticatedUser() throws Exception {
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
                  fileMock.getMimeType(),
                  "Content-Length",
                  String.valueOf(fileMock.getSize())),
              null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if X-Content-Disposition is missing returns"
            + " status code 400")
    void updateRoomPicture_testErrorMissingXContentDisposition() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.fromString(fileMock.getId())),
              fileMock.getFileBytes(),
              Collections.singletonMap("Content-Type", "application/octet-stream"),
              user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if user is not a room member returns status"
            + " code 403")
    void updateRoomPicture_testErrorUserIsNotRoomMember() throws Exception {
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
                  fileMock.getMimeType(),
                  "Content-Length",
                  String.valueOf(fileMock.getSize())),
              user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if user is not a room owner returns status"
            + " code 403")
    void updateRoomPicture_testErrorUserIsNotRoomOwner() throws Exception {
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
                  fileMock.getMimeType(),
                  "Content-Length",
                  String.valueOf(fileMock.getSize())),
              user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if the room isn't a group returns status code"
            + " 400")
    void updateRoomPicture_testErrorRoomIsNotRoomGroup() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if the image is too large returns status code"
            + " 400")
    void updateRoomPicture_testErrorImageTooLarge() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and a picture file, if the file isn't an image returns status code"
            + " 400")
    void updateRoomPicture_testErrorFileIsNotImage() throws Exception {
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
    }
  }

  @Nested
  @DisplayName("Delete room picture tests")
  class DeleteRoomPictureTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/picture", roomId);
    }

    @Test
    @DisplayName("Correctly deletes the room picture")
    void deleteRoomPicture_testOk() throws Exception {
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
      storageMockServer.mockDelete(fileMock.getId(), true);

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(204, response.getStatus());
      assertTrue(integrationTestUtils.getFileMetadataById(fileMock.getUUID()).isEmpty());
      assertNull(integrationTestUtils.getRoomById(roomId).orElseThrow().getPictureUpdatedAt());
    }

    @Test
    @DisplayName("If the user is not authenticated, it returns status code 401")
    void deleteRoomPicture_unauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If user is not a room owner, it returns status code 403")
    void deleteRoomPicture_userNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the room hasn't a picture, it returns a status code 404")
    void deleteRoomPicture_fileNotFound() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      UUID roomId = UUID.fromString(fileMock.getId());
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user1Token);
      assertEquals(404, response.getStatus());
      assertTrue(integrationTestUtils.getFileMetadataById(fileMock.getUUID()).isEmpty());
    }
  }

  @Nested
  @DisplayName("Mutes room for authenticated user tests")
  class MutesRoomForAuthenticatedUserTests {

    private final OffsetDateTime MUTED_TO_INFINITY = OffsetDateTime.parse("0001-01-01T00:00:00Z");

    private String url(UUID roomId) {
      return String.format("/rooms/%s/mute", roomId);
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings not exists")
    void muteRoom_testOkUserSettingNotExists() throws Exception {
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
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings exists")
    void muteRoom_testOkUserSettingExists() throws Exception {
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
    }

    @Test
    @DisplayName("Correctly does nothing if the user is already muted")
    void muteRoom_testOkUserAlreadyMuted() throws Exception {
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
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void muteRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void muteRoom_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), null, user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
    }
  }

  @Nested
  @DisplayName("Unmute room for authenticated user tests")
  class UnmuteRoomForAuthenticatedUserTests {

    private final OffsetDateTime MUTED_TO_INFINITY = OffsetDateTime.parse("0001-01-01T00:00:00Z");

    private String url(UUID roomId) {
      return String.format("/rooms/%s/mute", roomId);
    }

    @Test
    @DisplayName("Correctly does nothing if user settings not exists")
    void unmuteRoom_testOkUserSettingNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(204, response.getStatus());
      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user3Id);
      assertFalse(roomUserSettings.isPresent());
    }

    @Test
    @DisplayName("Unmute the current user in a specific room")
    void unmuteRoom_testOkUserSettingExists() throws Exception {
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
    }

    @Test
    @DisplayName("Correctly does nothing if the user has already unmuted")
    void unmuteRoom_testOkUserAlreadyUnmuted() throws Exception {
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
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void unmuteRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.delete(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void unmuteRoom_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
    }
  }

  @Nested
  @DisplayName("Clear room for authenticated user tests")
  class ClearRoomForAuthenticatedUserTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/clear", roomId);
    }

    @Test
    @DisplayName("Sets the clear date to now when user settings doesn't exist")
    void clearRoom_testOkUserSettingNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(200, response.getStatus());
      assertNotNull(
          objectMapper
              .readValue(response.getContentAsString(), ClearedDateDto.class)
              .getClearedAt());
    }

    @Test
    @DisplayName("Sets the clear date to now when user settings exists")
    void clearRoom_testOkUserSettingExists() throws Exception {
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
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void clearRoom_testAuthenticatedUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void clearRoom_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), null, user3Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
    }
  }

  @Nested
  @DisplayName("Gets list of room members tests")
  class GetsListOfRoomMembersTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/members", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns the list of room members")
    void listRoomMember_testOk() throws Exception {
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
    }

    @Test
    @DisplayName("Given a room identifier, if user isn't authenticated returns status code 401")
    void listRoomMember_testErrorUnauthenticatedUser() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.get(url(roomId), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if room doesn't exist returns status code 404")
    void listRoomMember_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if user isn't a room member returns status code 403")
    void listRoomMember_testErrorUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Insert room members tests")
  class InsertRoomMembersTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/members", roomId);
    }

    @Test
    @DisplayName("Given a group room identifier, correctly insert the new room members")
    void insertRoomMembers_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      List<MemberToInsertDto> requestMembers =
          List.of(MemberToInsertDto.create().userId(user4Id).historyCleared(false));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(requestMembers), user1Token);

      assertEquals(201, response.getStatus());
      List<MemberInsertedDto> members =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, members.size());
      MemberInsertedDto memberInsertedDto = members.get(0);
      assertEquals(user4Id, memberInsertedDto.getUserId());
      assertNull(memberInsertedDto.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .anyMatch(s -> user4Id.toString().equals(s.getUserId())));
    }

    @Test
    @DisplayName("Given a room identifier, if the room is one-to-one returns status code 400")
    void insertRoomMembers_oneToOneTest() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a group room identifier, correctly insert the new room members clearing the history")
    void insertRoomMembers_historyClearedOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
      List<MemberToInsertDto> requestMembers =
          List.of(MemberToInsertDto.create().userId(user4Id).historyCleared(true));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(requestMembers), user1Token);

      assertEquals(201, response.getStatus());
      List<MemberInsertedDto> members =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, members.size());
      MemberInsertedDto memberInsertedDto = members.get(0);
      assertEquals(user4Id, memberInsertedDto.getUserId());
      assertNotNull(memberInsertedDto.getClearedAt());

      Optional<Room> room = integrationTestUtils.getRoomById(roomId);
      assertTrue(room.isPresent());
      assertTrue(
          room.get().getSubscriptions().stream()
              .anyMatch(s -> user4Id.toString().equals(s.getUserId())));

      Optional<RoomUserSettings> roomUserSettings =
          integrationTestUtils.getRoomUserSettings(roomId, user4Id);
      assertTrue(roomUserSettings.isPresent());
      assertNotNull(roomUserSettings.get().getClearedAt());
    }

    @Test
    @DisplayName(
        "Given a room identifier, if there is one unauthenticated user returns status code 401")
    void insertRoomMembers_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(url(UUID.randomUUID()), getInsertRoomMemberRequestBody(user1Id), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exist returns status code 404")
    void insertRoomMembers_testRoomNotExist() throws Exception {
      UUID roomId = UUID.randomUUID();
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user1Id), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the authenticated user isn't a room owner returns status code"
            + " 403")
    void insertRoomMembers_testAuthenticateUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if one user is already a room member returns status code 400")
    void insertRoomMembers_testUserAlreadyRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user3Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if one user isn't an account returns status code 404")
    void insertRoomMembers_testUserNotHasAccount() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response =
          dispatcher.post(
              url(roomId), getInsertRoomMemberRequestBody(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a group room identifier, if the request doesn't contain historyCleared correctly"
            + " adds member with default historyCleared value")
    void insertRoomMembers_testHistoryClearedNotInitialized() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));

      MockHttpResponse response =
          dispatcher.post(url(roomId), getInsertRoomMemberRequestBody(user4Id), user1Token);

      // Native implementation handles default historyCleared value properly
      assertEquals(201, response.getStatus());
    }

    private String getInsertRoomMemberRequestBody(@Nullable UUID userId) {
      return getInsertRoomMemberRequestBody(List.of(MemberToInsertDto.create().userId(userId)));
    }

    private String getInsertRoomMemberRequestBody(@Nullable List<MemberToInsertDto> members) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[");
      Optional.ofNullable(members)
          .ifPresent(
              memberList ->
                  stringBuilder.append(
                      memberList.stream()
                          .map(
                              member ->
                                  String.format(
                                      "{\"userId\": \"%s\", \"owner\": \"%s\", \"historyCleared\":"
                                          + " \"%s\"}",
                                      member.getUserId(),
                                      member.isOwner(),
                                      member.isHistoryCleared()))
                          .collect(Collectors.joining(","))));
      if (',' == stringBuilder.charAt(stringBuilder.length() - 1)) {
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      }
      stringBuilder.append("]");
      return stringBuilder.toString();
    }
  }

  @Nested
  @DisplayName("Remove a member from the room tests")
  class RemoveRoomMemberTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s", roomId, userId);
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, correctly remove the user from room"
            + " members")
    void deleteRoomMember_groupTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));

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
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, "
            + "correctly leaves the user from the from room members")
    void deleteRoomMember_memberIsNotActiveMeetingParticipantTestOk() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, correctly leaves the user from the"
            + " associated meeting and removes the user from room members")
    void deleteRoomMember_memberIsActiveMeetingParticipantTestOk() throws Exception {
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
      meetingTestUtils.insertVideoServerSession(
          videoServerMeeting,
          user2Id.toString(),
          user2Queue,
          "connection_" + user2Queue,
          "audioHandleId_" + user2Queue,
          "videoOutHandleId_" + user2Queue,
          "videoInHandleId_" + user2Queue,
          "screenHandleId_" + user2Queue);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/videoInHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/videoOutHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/screenHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue,
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

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
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier, correctly leaves the user from the"
            + " associated meeting, removes the meeting because the user was the last and removes"
            + " the user from room members")
    void deleteRoomMember_memberIsLastMeetingParticipantTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      String user2Queue = UUID.randomUUID().toString();
      Room roomEntity =
          integrationTestUtils.generateAndSaveRoom(
              roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              roomId,
              MeetingType.PERMANENT,
              List.of(ParticipantBuilder.create(user2Id, user2Queue)),
              true);
      integrationTestUtils.updateRoom(roomEntity.meetingId(meetingId.toString()));

      meetingTestUtils.insertVideoServerSession(
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
          "audioHandleId_" + user2Queue,
          "videoOutHandleId_" + user2Queue,
          "videoInHandleId_" + user2Queue,
          "screenHandleId_" + user2Queue);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/audioHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/videoInHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/videoOutHandleId_" + user2Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user2Queue + "/screenHandleId_" + user2Queue,
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if there isn't an authenticated user"
            + " returns status code 401")
    void deleteRoomMember_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user1Id), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if room doesn't exist returns status code"
            + " 404")
    void deleteRoomMember_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user1Id), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the authenticated user isn't a room"
            + " owner returns status code 403")
    void deleteRoomMember_testErrorUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user3Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room is a one-to-one returns"
            + " status code 403")
    void deleteRoomMember_testErrorRoomOneToOne() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.ONE_TO_ONE, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a group room identifier and a member identifier equals to authenticated user,"
            + " correctly user remove itself")
    void deleteRoomMember_userRemoveItselfTestOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId,
          RoomTypeDto.GROUP,
          "room",
          List.of(user1Id, user2Id, user3Id),
          List.of(user1Id),
          null,
          null);
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
    }
  }

  @Nested
  @DisplayName("Promote a member to owner tests")
  class PromoteMemberToOwnerTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s/owner", roomId, userId);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, correctly promotes the member to owner")
    void updateToOwner_testOk() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if there isn't an authenticated user"
            + " return status code 401")
    void updateToOwner_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID(), user1Id), null, null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room doesn't exist return status"
            + " code 404")
    void updateToOwner_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID(), user2Id), null, user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the authenticated user isn't a room"
            + " owner return status code 403")
    void updateToOwner_testAuthenticateUserNotRoomOwner() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user2Id), null, user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the member doesn't exist return status"
            + " code 403")
    void updateToOwner_testErrorMemberNotExists() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      MockHttpResponse response = dispatcher.put(url(roomId, user4Id), null, user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room is a one-to-one then it"
            + " returns status code 400")
    void updateToOwner_testErrorRoomIsOneToOne() throws Exception {
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
    }
  }

  @Nested
  @DisplayName("Demote a member from owner to normal member tests")
  class DemoteOwnerTests {

    private String url(UUID roomId, UUID userId) {
      return String.format("/rooms/%s/members/%s/owner", roomId, userId);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, correctly demotes a owner to normal"
            + " member")
    void deleteOwner_testOk() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if there isn't an authenticated user"
            + " returns status code 401")
    void deleteOwner_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), user2Id), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if authenticated user isn't a room owner"
            + " returns status code 403")
    void deleteOwner_testErrorAuthenticatedUserNotRoomOwner() throws Exception {
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
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if requested user isn't a room member"
            + " returns status code 403")
    void deleteOwner_testErrorUserNotRoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user3Id));
      MockHttpResponse response = dispatcher.delete(url(roomId, user2Id), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier and a member identifier, if the room is a one-to-one then it"
            + " returns status code 400")
    void deleteOwner_testErrorRoomIsOneToOne() throws Exception {
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
    }
  }

  @Nested
  @DisplayName("Gets paged list of room attachment information tests")
  class ListRoomAttachmentInformationTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/attachments", roomId);
    }

    private void seedMixedAttachments(UUID roomId) {
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), "png", "image/png", FileMetadataType.ATTACHMENT, user1Id, roomId);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), "jpeg", "image/jpeg", FileMetadataType.ATTACHMENT, user1Id, roomId);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), "mp4", "video/mp4", FileMetadataType.ATTACHMENT, user1Id, roomId);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), "mpeg", "audio/mpeg", FileMetadataType.ATTACHMENT, user1Id, roomId);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(),
          "pdf",
          "application/pdf",
          FileMetadataType.ATTACHMENT,
          user1Id,
          roomId);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), "txt", "text/plain", FileMetadataType.ATTACHMENT, user1Id, roomId);
    }

    @Test
    @DisplayName(
        "Given a room identifier, correctly returns a single paged list of attachments info of the"
            + " required room")
    void listRoomAttachmentInfo_testOkSinglePage() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("faec1132-567d-451c-a969-18ca9131bdfa"),
              "Test attachment 1",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("6a6a1f06-0947-4b5f-a6ac-7631426e3a62"),
              "Test attachment 2",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("991b5178-1108-459e-a017-4197647167ec"),
              "Test attachment 3",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      FileMetadata file4 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("5a3d8dd2-f431-4195-acc1-5108948c6d26"),
              "Test attachment 4",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.removeFixTime();

      MockHttpResponse response = dispatcher.get(url(room1Id), user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto attachments =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(4, attachments.getAttachments().size());
      assertEquals(4L, attachments.getTotal());
      assertEquals(
          List.of(file3.getId(), file4.getId(), file1.getId(), file2.getId()),
          attachments.getAttachments().stream()
              .map(attachment -> attachment.getId().toString())
              .toList());
    }

    @Test
    @DisplayName(
        "Given a room identifier, correctly returns multiple paged lists of attachments info of the"
            + " required room")
    void listRoomAttachmentInfo_testOkMultiplePages() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("faec1132-567d-451c-a969-18ca9131bdfa"),
              "Test attachment 1",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("6a6a1f06-0947-4b5f-a6ac-7631426e3a62"),
              "Test attachment 2",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("991b5178-1108-459e-a017-4197647167ec"),
              "Test attachment 3",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.fixTimeAt(Instant.parse("2021-07-05T00:00:00Z"));
      FileMetadata file4 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("5a3d8dd2-f431-4195-acc1-5108948c6d26"),
              "Test attachment 4",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.removeFixTime();

      MockHttpResponse response1 =
          dispatcher.get(String.join("", url(room1Id), "?limit=2"), user1Token);
      assertEquals(200, response1.getStatus());
      AttachmentsPaginationDto attachmentsPage1 =
          objectMapper.readValue(response1.getContentAsString(), AttachmentsPaginationDto.class);
      assertEquals(2, attachmentsPage1.getAttachments().size());
      assertEquals(4L, attachmentsPage1.getTotal());
      assertEquals(
          List.of(file3.getId(), file1.getId()),
          attachmentsPage1.getAttachments().stream()
              .map(attachment -> attachment.getId().toString())
              .toList());
      assertNotNull(attachmentsPage1.getCursor());

      MockHttpResponse response2 =
          dispatcher.get(
              String.join("", url(room1Id), "?limit=2&cursor=", attachmentsPage1.getCursor()),
              user1Token);
      assertEquals(200, response2.getStatus());
      AttachmentsPaginationDto attachmentsPage2 =
          objectMapper.readValue(response2.getContentAsString(), AttachmentsPaginationDto.class);
      assertEquals(2, attachmentsPage2.getAttachments().size());
      assertEquals(4L, attachmentsPage2.getTotal());
      assertEquals(
          List.of(file4.getId(), file2.getId()),
          attachmentsPage2.getAttachments().stream()
              .map(attachment -> attachment.getId().toString())
              .toList());
      assertNull(attachmentsPage2.getCursor());
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user is not authenticated return a status code 401")
    void listRoomAttachmentInfo_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if authenticated user isn't a room member then return a status"
            + " code 403")
    void listRoomAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.get(url(roomId), user3Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given an IMAGES mimeTypeCategory filter, returns only images")
    void listRoomAttachmentInfo_filterByImagesCategory() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      seedMixedAttachments(room1Id);

      MockHttpResponse response =
          dispatcher.get(url(room1Id) + "?mimeTypeCategory=IMAGES", user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2L, result.getTotal());
      assertEquals(
          List.of("image/jpeg", "image/png"),
          result.getAttachments().stream().map(a -> a.getMimeType()).sorted().toList());
    }

    @Test
    @DisplayName("Given a VIDEOS mimeTypeCategory filter, returns only videos")
    void listRoomAttachmentInfo_filterByVideosCategory() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      seedMixedAttachments(room1Id);

      MockHttpResponse response =
          dispatcher.get(url(room1Id) + "?mimeTypeCategory=VIDEOS", user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1L, result.getTotal());
      assertEquals(
          List.of("video/mp4"),
          result.getAttachments().stream().map(a -> a.getMimeType()).sorted().toList());
    }

    @Test
    @DisplayName(
        "Given a DOCUMENTS mimeTypeCategory filter, returns every other MIME type incl. audio")
    void listRoomAttachmentInfo_filterByDocumentsCategory() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      seedMixedAttachments(room1Id);

      MockHttpResponse response =
          dispatcher.get(url(room1Id) + "?mimeTypeCategory=DOCUMENTS", user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(3L, result.getTotal());
      assertEquals(
          List.of("application/pdf", "audio/mpeg", "text/plain"),
          result.getAttachments().stream().map(a -> a.getMimeType()).sorted().toList());
    }

    @Test
    @DisplayName("Given both mimeType and mimeTypeCategory filters, returns status code 400")
    void listRoomAttachmentInfo_mimeTypeAndCategoryMutuallyExclusive() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response =
          dispatcher.get(
              url(room1Id) + "?mimeType=image%2Fpng&mimeTypeCategory=IMAGES", user1Token);

      assertEquals(400, response.getStatus());
    }

    @Test
    @DisplayName(
        "Pagination cursor correctly includes items whose ID is lexicographically greater than the"
            + " cursor item's ID when they have an older timestamp")
    void listRoomAttachmentInfo_cursorIncludesItemsWithLargerIdOnNextPage() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      // Page 1 will end with this item (small ID, newer timestamp → cursor encodes this)
      clock.fixTimeAt(Instant.parse("2022-06-01T00:00:00Z"));
      FileMetadata smallIdFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("1aaaaaaa-0000-0000-0000-000000000000"),
              "small id file",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);

      // Page 2 must include this item: ID > cursor ID but timestamp < cursor timestamp
      // The buggy query would skip it because `id < cursor_id` fails for 'f' > '1'
      clock.fixTimeAt(Instant.parse("2021-01-01T00:00:00Z"));
      FileMetadata largeIdFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.fromString("faaaaaaa-0000-0000-0000-000000000000"),
              "large id file",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.removeFixTime();

      MockHttpResponse response1 = dispatcher.get(url(room1Id) + "?limit=1", user1Token);
      assertEquals(200, response1.getStatus());
      AttachmentsPaginationDto page1 =
          objectMapper.readValue(response1.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, page1.getAttachments().size());
      assertEquals(smallIdFile.getId(), page1.getAttachments().get(0).getId().toString());
      assertNotNull(page1.getCursor());

      MockHttpResponse response2 =
          dispatcher.get(url(room1Id) + "?limit=1&cursor=" + page1.getCursor(), user1Token);
      assertEquals(200, response2.getStatus());
      AttachmentsPaginationDto page2 =
          objectMapper.readValue(response2.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, page2.getAttachments().size());
      assertEquals(largeIdFile.getId(), page2.getAttachments().get(0).getId().toString());
      assertNull(page2.getCursor());
    }

    @Test
    @DisplayName("Given a userId filter, returns only that user's attachments")
    void listRoomAttachmentInfo_filterByUserId() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(),
          "user1 attachment 1",
          "image/png",
          FileMetadataType.ATTACHMENT,
          user1Id,
          room1Id);
      FileMetadata file2 =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "user2 attachment",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user2Id,
              room1Id);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(),
          "user1 attachment 2",
          "image/png",
          FileMetadataType.ATTACHMENT,
          user1Id,
          room1Id);

      MockHttpResponse response = dispatcher.get(url(room1Id) + "?userId=" + user2Id, user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, result.getAttachments().size());
      assertEquals(1L, result.getTotal());
      assertEquals(file2.getId(), result.getAttachments().get(0).getId().toString());
    }

    @Test
    @DisplayName("Given an exact mimeType filter, returns only matching attachments")
    void listRoomAttachmentInfo_filterByExactMimeType() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      FileMetadata pdfFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "document",
              "application/pdf",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), "image", "image/png", FileMetadataType.ATTACHMENT, user1Id, room1Id);

      MockHttpResponse response =
          dispatcher.get(url(room1Id) + "?mimeType=application%2Fpdf", user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, result.getAttachments().size());
      assertEquals(pdfFile.getId(), result.getAttachments().get(0).getId().toString());
    }

    @Test
    @DisplayName("Given a mimeType prefix filter ending with '/', returns all matching attachments")
    void listRoomAttachmentInfo_filterByMimeTypePrefix() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      FileMetadata imagePng =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "image png",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      FileMetadata imageJpeg =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "image jpeg",
              "image/jpeg",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(),
          "pdf doc",
          "application/pdf",
          FileMetadataType.ATTACHMENT,
          user1Id,
          room1Id);

      MockHttpResponse response = dispatcher.get(url(room1Id) + "?mimeType=image%2F", user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, result.getAttachments().size());
      assertEquals(2L, result.getTotal());
      assertTrue(
          result.getAttachments().stream()
              .map(a -> a.getId().toString())
              .toList()
              .containsAll(List.of(imagePng.getId(), imageJpeg.getId())));
    }

    @Test
    @DisplayName("Given timestamp range filters, returns only in-range attachments")
    void listRoomAttachmentInfo_filterByTimestampRange() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2021-01-01T00:00:00Z"));
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(),
          "old file",
          "image/png",
          FileMetadataType.ATTACHMENT,
          user1Id,
          room1Id);

      clock.fixTimeAt(Instant.parse("2022-06-15T00:00:00Z"));
      FileMetadata inRangeFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "in-range file",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);

      clock.fixTimeAt(Instant.parse("2023-12-31T00:00:00Z"));
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(),
          "new file",
          "image/png",
          FileMetadataType.ATTACHMENT,
          user1Id,
          room1Id);
      clock.removeFixTime();

      MockHttpResponse response =
          dispatcher.get(
              url(room1Id)
                  + "?createdAfter=2022-01-01T00:00:00Z&createdBefore=2023-01-01T00:00:00Z",
              user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, result.getAttachments().size());
      assertEquals(inRangeFile.getId(), result.getAttachments().get(0).getId().toString());
    }

    @Test
    @DisplayName("Given size range filters, returns only attachments within the size range")
    void listRoomAttachmentInfo_filterByMinAndMaxSize() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      fileMetadataRepository.save(
          FileMetadata.create()
              .id(UUID.randomUUID().toString())
              .name("small file")
              .originalSize(100L)
              .mimeType("image/png")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(room1Id.toString()));
      FileMetadata mediumFile =
          fileMetadataRepository.save(
              FileMetadata.create()
                  .id(UUID.randomUUID().toString())
                  .name("medium file")
                  .originalSize(5000L)
                  .mimeType("image/png")
                  .type(FileMetadataType.ATTACHMENT)
                  .userId(user1Id.toString())
                  .roomId(room1Id.toString()));
      fileMetadataRepository.save(
          FileMetadata.create()
              .id(UUID.randomUUID().toString())
              .name("large file")
              .originalSize(100000L)
              .mimeType("image/png")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(room1Id.toString()));

      MockHttpResponse response =
          dispatcher.get(url(room1Id) + "?minSize=500&maxSize=10000", user1Token);

      assertEquals(200, response.getStatus());
      AttachmentsPaginationDto result =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, result.getAttachments().size());
      assertEquals(mediumFile.getId(), result.getAttachments().get(0).getId().toString());
    }

    @Test
    @DisplayName(
        "Given sortBy=size and order=desc, returns attachments sorted by size descending"
            + " with correct pagination cursor")
    void listRoomAttachmentInfo_sortBySizeDescWithPagination() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      FileMetadata largeFile =
          fileMetadataRepository.save(
              FileMetadata.create()
                  .id(UUID.fromString("c1000000-0000-0000-0000-000000000001").toString())
                  .name("large file")
                  .originalSize(10000L)
                  .mimeType("image/png")
                  .type(FileMetadataType.ATTACHMENT)
                  .userId(user1Id.toString())
                  .roomId(room1Id.toString()));
      FileMetadata mediumFile =
          fileMetadataRepository.save(
              FileMetadata.create()
                  .id(UUID.fromString("b1000000-0000-0000-0000-000000000001").toString())
                  .name("medium file")
                  .originalSize(5000L)
                  .mimeType("image/png")
                  .type(FileMetadataType.ATTACHMENT)
                  .userId(user1Id.toString())
                  .roomId(room1Id.toString()));
      FileMetadata smallFile =
          fileMetadataRepository.save(
              FileMetadata.create()
                  .id(UUID.fromString("a1000000-0000-0000-0000-000000000001").toString())
                  .name("small file")
                  .originalSize(100L)
                  .mimeType("image/png")
                  .type(FileMetadataType.ATTACHMENT)
                  .userId(user1Id.toString())
                  .roomId(room1Id.toString()));

      MockHttpResponse response1 =
          dispatcher.get(url(room1Id) + "?limit=2&sortBy=size&order=desc", user1Token);

      assertEquals(200, response1.getStatus());
      AttachmentsPaginationDto page1 =
          objectMapper.readValue(response1.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, page1.getAttachments().size());
      assertEquals(
          List.of(largeFile.getId(), mediumFile.getId()),
          page1.getAttachments().stream().map(a -> a.getId().toString()).toList());
      assertNotNull(page1.getCursor());

      MockHttpResponse response2 =
          dispatcher.get(
              url(room1Id) + "?limit=2&sortBy=size&order=desc&cursor=" + page1.getCursor(),
              user1Token);

      assertEquals(200, response2.getStatus());
      AttachmentsPaginationDto page2 =
          objectMapper.readValue(response2.getContentAsString(), new TypeReference<>() {});
      assertEquals(1, page2.getAttachments().size());
      assertEquals(smallFile.getId(), page2.getAttachments().get(0).getId().toString());
      assertNull(page2.getCursor());
    }

    @Test
    @DisplayName(
        "Given sortBy=created_at, returns attachments sorted by creation date in the requested"
            + " order")
    void listRoomAttachmentInfo_sortByCreatedAt() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2021-01-01T00:00:00Z"));
      FileMetadata oldestFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "oldest file",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);

      clock.fixTimeAt(Instant.parse("2022-06-15T00:00:00Z"));
      FileMetadata middleFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "middle file",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);

      clock.fixTimeAt(Instant.parse("2023-12-31T00:00:00Z"));
      FileMetadata newestFile =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "newest file",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);
      clock.removeFixTime();

      MockHttpResponse ascResponse =
          dispatcher.get(url(room1Id) + "?sortBy=created_at&order=asc", user1Token);
      assertEquals(200, ascResponse.getStatus());
      AttachmentsPaginationDto ascResult =
          objectMapper.readValue(ascResponse.getContentAsString(), new TypeReference<>() {});
      assertEquals(3, ascResult.getAttachments().size());
      assertEquals(
          List.of(oldestFile.getId(), middleFile.getId(), newestFile.getId()),
          ascResult.getAttachments().stream().map(a -> a.getId().toString()).toList());

      MockHttpResponse descResponse =
          dispatcher.get(url(room1Id) + "?sortBy=created_at&order=desc", user1Token);
      assertEquals(200, descResponse.getStatus());
      AttachmentsPaginationDto descResult =
          objectMapper.readValue(descResponse.getContentAsString(), new TypeReference<>() {});
      assertEquals(3, descResult.getAttachments().size());
      assertEquals(
          List.of(newestFile.getId(), middleFile.getId(), oldestFile.getId()),
          descResult.getAttachments().stream().map(a -> a.getId().toString()).toList());
    }
  }

  @Nested
  @DisplayName("Insert attachment multipart tests")
  class InsertAttachmentMultipartTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/attachments", roomId);
    }

    @Test
    @DisplayName("Given a room identifier and an attachment, correctly inserts the attachment")
    void insertAttachmentMultipart_testOk() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      storageMockServer.mockUpload(
          fileMock,
          new StorageMockServer.UploadResponse()
              .digest("")
              .digestAlgorithm("")
              .size(fileMock.getSize()),
          true);

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.postMultipart(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "mimeType",
                    fileMock.getMimeType(),
                    "contentLength",
                    String.valueOf(fileMock.getSize())),
                StringFormatUtils.encodeToUtf8(fileMock.getName()),
                ContentType.parse(fileMock.getMimeType()),
                user1Token);
      }

      assertEquals(201, response.getStatus());

      MessageDto attachmentMessage =
          objectMapper.readValue(response.getContentAsString(), MessageDto.class);

      assertTrue(
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .stream()
              .anyMatch(
                  attach -> attach.getId().equals(attachmentMessage.getAttachmentId().toString())));
    }

    @Test
    @DisplayName(
        "Given a room identifier and an attachment, correctly inserts the attachment with a"
            + " description")
    void insertAttachmentMultipart_testOkWithDescription() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id, user3Id));
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      storageMockServer.mockUpload(
          fileMock,
          new StorageMockServer.UploadResponse()
              .digest("")
              .digestAlgorithm("")
              .size(fileMock.getSize()),
          true);

      MockHttpResponse response;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(fileMock.getUUID());
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        response =
            dispatcher.postMultipart(
                url(roomId),
                fileMock.getFileBytes(),
                Map.of(
                    "description",
                    fileMock.getName().replace(".jpg", ""),
                    "mimeType",
                    fileMock.getMimeType(),
                    "contentLength",
                    String.valueOf(fileMock.getSize())),
                StringFormatUtils.encodeToUtf8(fileMock.getName()),
                ContentType.parse(fileMock.getMimeType()),
                user1Token);
      }

      assertEquals(201, response.getStatus());

      MessageDto attachmentMessage =
          objectMapper.readValue(response.getContentAsString(), MessageDto.class);

      assertTrue(
          integrationTestUtils
              .getFileMetadataByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT)
              .stream()
              .anyMatch(
                  attach -> attach.getId().equals(attachmentMessage.getAttachmentId().toString())));
    }
  }

  @Nested
  @DisplayName("Get meeting by room id tests")
  class GetMeetingByRoomIdTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/meeting", roomId);
    }

    @Test
    @DisplayName(
        "Given a room identifier, correctly returns the room meeting information with participants")
    void getMeetingByRoomId_testOk() throws Exception {
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
    void getMeetingByRoomId_testMeetingNotExists() throws Exception {
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
    void getMeetingByRoomId_testUserIsNotRoomMember() throws Exception {
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
    void getMeetingByRoomId_testRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user isn’t authenticated then it returns a status code"
            + " 401")
    void getMeetingByRoomId_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Bulk delete room attachments tests")
  class BulkDeleteRoomAttachmentsTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/attachments", roomId);
    }

    @Test
    @DisplayName("Room member bulk deletes their own attachments in the room, returns 200")
    void bulkDeleteRoomAttachments_testOk() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);

      // storages bulkDelete returns the FAILED (not-deleted) ids: empty => all deleted
      storageMockServer.setBulkDeleteResponse(List.of(file1.getId()), List.of());

      String requestBody =
          objectMapper.writeValueAsString(
              BulkDeleteAttachmentsRequestDto.create()
                  .attachmentIds(List.of(UUID.fromString(file1.getId()))));

      MockHttpResponse response = dispatcher.delete(url(room1Id), requestBody, user1Token);

      assertEquals(200, response.getStatus());
      BulkDeleteAttachmentsResponseDto result =
          objectMapper.readValue(
              response.getContentAsString(), BulkDeleteAttachmentsResponseDto.class);
      assertEquals(List.of(UUID.fromString(file1.getId())), result.getSuccessIds());
      assertTrue(result.getFailedIds().isEmpty());
      assertTrue(fileMetadataRepository.getById(file1.getId()).isEmpty());
    }

    @Test
    @DisplayName("Non-member trying to bulk delete returns 403")
    void bulkDeleteRoomAttachments_testNotMember() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      String requestBody =
          objectMapper.writeValueAsString(
              BulkDeleteAttachmentsRequestDto.create().attachmentIds(List.of(UUID.randomUUID())));

      MockHttpResponse response = dispatcher.delete(url(room1Id), requestBody, user3Token);

      assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Member trying to delete another user's attachment returns 403")
    void bulkDeleteRoomAttachments_testNotOwner() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room1Id);

      String requestBody =
          objectMapper.writeValueAsString(
              BulkDeleteAttachmentsRequestDto.create()
                  .attachmentIds(List.of(UUID.fromString(file1.getId()))));

      MockHttpResponse response = dispatcher.delete(url(room1Id), requestBody, user3Token);

      assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Bulk delete with a non-existent or inaccessible file id returns 403")
    void bulkDeleteRoomAttachments_testFileNotFound() throws Exception {
      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      String requestBody =
          objectMapper.writeValueAsString(
              BulkDeleteAttachmentsRequestDto.create().attachmentIds(List.of(UUID.randomUUID())));

      MockHttpResponse response = dispatcher.delete(url(room1Id), requestBody, user1Token);

      assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Bulk delete with a file that belongs to a different room returns 403")
    void bulkDeleteRoomAttachments_testFileNotInRoom() throws Exception {
      UUID room1Id = UUID.randomUUID();
      UUID room2Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));
      integrationTestUtils.generateAndSaveRoom(
          room2Id, RoomTypeDto.GROUP, "room2", List.of(user1Id, user2Id));
      FileMetadata file1 =
          integrationTestUtils.generateAndSaveFileMetadata(
              MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
              FileMetadataType.ATTACHMENT,
              user1Id,
              room2Id);

      String requestBody =
          objectMapper.writeValueAsString(
              BulkDeleteAttachmentsRequestDto.create()
                  .attachmentIds(List.of(UUID.fromString(file1.getId()))));

      MockHttpResponse response = dispatcher.delete(url(room1Id), requestBody, user1Token);

      assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void bulkDeleteRoomAttachments_testUnauthenticated() throws Exception {
      String requestBody =
          objectMapper.writeValueAsString(
              BulkDeleteAttachmentsRequestDto.create().attachmentIds(List.of(UUID.randomUUID())));

      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), requestBody, null);

      assertEquals(401, response.getStatus());
    }
  }
}
