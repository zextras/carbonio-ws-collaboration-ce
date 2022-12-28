// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils.RoomMemberField;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.ParticipantDto;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class MeetingApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final MeetingRepository         meetingRepository;
  private final ParticipantRepository     participantRepository;
  private final MeetingTestUtils          meetingTestUtils;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final UserManagementMockServer  userManagementMockServer;
  private final AppClock                  clock;

  public MeetingApiIT(
    RoomsApi roomsApi, ResteasyRequestDispatcher dispatcher,
    MeetingRepository meetingRepository,
    ParticipantRepository participantRepository,
    MeetingTestUtils meetingTestUtils,
    ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    UserManagementMockServer userManagementMockServer, Clock clock
  ) {
    this.dispatcher = dispatcher;
    this.meetingRepository = meetingRepository;
    this.participantRepository = participantRepository;
    this.meetingTestUtils = meetingTestUtils;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.clock = (AppClock) clock;
  }

  private static UUID   user1Id;
  private static String user1Token;
  private static String user1session1;
  private static UUID   user2Id;
  private static String user2Token;
  private static String user2session1;
  private static String user2session2;
  private static UUID   user3Id;
  private static String user3session1;
  private static UUID   room1Id;
  private static UUID   room2Id;
  private static UUID   room3Id;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    user1Token = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    user1session1 = "user1session1";
    user2Id = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    user2Token = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
    user2session1 = "user2session1";
    user2session2 = "user2session2";
    user3Id = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
    user3session1 = "user3session1";
    room1Id = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
    room2Id = UUID.fromString("0367dedb-a5d8-451f-bbc8-22e70d8a777a");
    room3Id = UUID.fromString("e110c21d-8c73-4096-b449-166264399ac8");
  }

  @Nested
  @DisplayName("List meetings tests")
  public class ListMeetingTests {

    private static final String URL = "/meetings";

    @Test
    @DisplayName("Correctly gets the meetings of authenticated user")
    public void listMeeting_testOk() throws Exception {
      UUID meeting1Id = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      UUID meeting2Id = UUID.fromString("4b592aa4-0d04-46d5-8292-953e4ed4247e");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("room1hash").name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room2Id.toString()).type(RoomTypeDto.GROUP).hash("room2hash").name("room2")
          .description("Room two"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room3Id.toString()).type(RoomTypeDto.GROUP).hash("room3hash").name("room3")
          .description("Room three"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meeting1Id, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));
      meetingTestUtils.generateAndSaveMeeting(meeting2Id, room2Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(true)));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<MeetingDto> meetings = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertNotNull(meetings);
      assertEquals(2, meetings.size());
      MeetingDto meeting1Dto = meetings.stream().filter(m -> m.getId().equals(meeting1Id)).findAny().orElseThrow();
      assertEquals(meeting1Id, meeting1Dto.getId());
      assertEquals(room1Id, meeting1Dto.getRoomId());
      assertNotNull(meeting1Dto.getParticipants());
      assertEquals(4, meeting1Dto.getParticipants().size());
      assertEquals(1, (int) meeting1Dto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).count());
      assertEquals(2, (int) meeting1Dto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meeting1Dto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      Optional<ParticipantDto> participant = meeting1Dto.getParticipants().stream()
        .filter(p -> user1Id.equals(p.getUserId())).findAny();
      assertTrue(participant.isPresent());
      assertEquals(user1Id, participant.get().getUserId());
      assertEquals(user1session1, participant.get().getSessionId());
      assertTrue(participant.get().isVideoStreamOn());
      assertTrue(participant.get().isAudioStreamOn());

      MeetingDto meeting2Dto = meetings.stream().filter(m -> m.getId().equals(meeting2Id)).findAny().orElseThrow();
      assertEquals(meeting2Id, meeting2Dto.getId());
      assertEquals(room2Id, meeting2Dto.getRoomId());
      assertNotNull(meeting2Dto.getParticipants());
      assertEquals(2, meeting2Dto.getParticipants().size());
      assertEquals(1, (int) meeting2Dto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).count());
      assertEquals(1, (int) meeting2Dto.getParticipants().stream()
        .filter(p -> user3Id.equals(p.getUserId())).count());
      participant = meeting2Dto.getParticipants().stream()
        .filter(p -> user2Id.equals(p.getUserId())).findAny();
      assertTrue(participant.isPresent());
      assertEquals(user2Id, participant.get().getUserId());
      assertEquals(user2session1, participant.get().getSessionId());
      assertFalse(participant.get().isVideoStreamOn());
      assertTrue(participant.get().isAudioStreamOn());
    }

    @Test
    @DisplayName("If rooms, which user is member of, hasn't any meetings, it returns an empty list")
    public void listMeeting_testUserRoomsHasNoMeetings() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("room1hash").name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room2Id.toString()).type(RoomTypeDto.GROUP).hash("room2hash").name("room2")
          .description("Room two"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room3Id.toString()).type(RoomTypeDto.GROUP).hash("room3hash").name("room3")
          .description("Room three"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<MeetingDto> meetings = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertNotNull(meetings);
      assertEquals(0, meetings.size());
    }

    @Test
    @DisplayName("If the user is not a member of any room, correctly gets an empty list")
    public void listMeeting_testUserHasNotRooms() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("room1hash").name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room2Id.toString()).type(RoomTypeDto.GROUP).hash("room2hash").name("room2")
          .description("Room two"),
        List.of(
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id).owner(true)));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<MeetingDto> meetings = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertNotNull(meetings);
      assertEquals(0, meetings.size());
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void listMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(URL, null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Get meeting by id tests")
  public class GetMeetingByIdTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s", meetingId);
    }

    @Test
    @DisplayName("Given a meeting identifier, correctly returns the meeting information with participants")
    public void getMeetingById_testOk() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.get(url(meetingId), user1Token);

      assertEquals(200, response.getStatus());
      MeetingDto meetingDto = objectMapper.readValue(response.getContentAsString(), MeetingDto.class);
      assertNotNull(meetingDto);
      assertEquals(meetingId, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());
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
      assertEquals(user1session1, participant1.get().getSessionId());
      assertTrue(participant1.get().isVideoStreamOn());
      assertTrue(participant1.get().isAudioStreamOn());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void getMeetingById_testUserIsNotRoomMember() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.get(url(meetingId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void getMeetingById_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user isn’t authenticated then it returns a status code 401")
    public void getMeetingById_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Delete meeting by id tests")
  public class DeleteMeetingByIdTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s", meetingId);
    }

    @Test
    @DisplayName("Given a meeting identifier, correctly deletes the meeting and the participants")
    public void deleteMeetingById_testOk() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertEquals(0, participantRepository.getByMeetingId(meetingId.toString()).size());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void deleteMeetingById_testUserIsNotRoomMember() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isPresent());
      assertEquals(3, participantRepository.getByMeetingId(meetingId.toString()).size());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void deleteMeetingById_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if the user isn’t authenticated then it returns a status code 401")
    public void getMeetingById_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Join meeting Tests")
  public class JoinMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/join", meetingId);
    }

    @Test
    @DisplayName("Given a meeting identifier, the authenticated user correctly joins to the meeting")
    public void joinMeeting_testOk() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(url(meetingId),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(4, meeting.getParticipants().size());
      Participant newParticipant = meeting.getParticipants().stream().filter(participant ->
        user1Id.toString().equals(participant.getUserId()) && user1session1.equals(participant.getSessionId())
      ).findAny().orElseThrow();
      assertTrue(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("Given a meeting identifier, the authenticated user correctly joins to the meeting with another session")
    public void joinMeeting_testOkSameUserAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(url(meetingId),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user2session2), user2Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(4, meeting.getParticipants().size());
      Participant newParticipant = meeting.getParticipants().stream().filter(participant ->
        user2Id.toString().equals(participant.getUserId()) && user2session2.equals(participant.getSessionId())
      ).findAny().orElseThrow();
      assertTrue(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void joinMeeting_testUserIsNotRoomMember() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(url(meetingId),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void joinMeeting_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user isn’t authenticated then it returns a status code 401")
    public void joinMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user1session1), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Leave meeting tests")
  public class LeaveMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/leave", meetingId);
    }

    @Test
    @DisplayName("Given a meeting identifier, the authenticated user correctly leaves the meeting")
    public void leaveMeeting_testOk() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user2session2), user2Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertTrue(meeting.getParticipants().stream().filter(participant ->
        user2Id.toString().equals(participant.getUserId()) &&
          user2session2.equals(participant.getSessionId())
      ).findAny().isEmpty());
    }

    @Test
    @DisplayName("Given a meeting identifier, " +
      "the authenticated user correctly leaves the meeting as last participant and the meeting is closed")
    public void leaveMeeting_testOkLastParticipant() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      Room room = integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertNull(integrationTestUtils.getRoomById(room1Id).orElseThrow().getMeetingId());

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the authenticated user isn't a meeting participant then it returns a status code 404")
    public void leaveMeeting_testIsNotMeetingParticipant() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void leaveMeeting_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user isn’t authenticated then it returns a status code 401")
    public void leaveMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(JoinSettingsDto.create().audioStreamOn(true).videoStreamOn(false)),
        Map.of("session-id", user1session1), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable video stream tests")
  public class EnableVideoStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/video", meetingId);
    }

    @Test
    @DisplayName("Video stream correctly opened for the current session and it returns a status code 204")
    public void enableVideoStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If video stream is already opened for the current session, correctly it ignores and returns a status code 204")
    public void enableVideoStream_testOkVideoStreamAlreadyOpenWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void enableVideoStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void enableVideoStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher
        .put(url(UUID.randomUUID()), (String) null, Map.of("session-id", user1session1), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the current user doesn't have the session identifier, it returns a status code 400")
    public void enableVideoStream_testErrorCurrentUserWithoutSessionId() throws Exception {
      MockHttpResponse response = dispatcher
        .put(url(UUID.randomUUID()), (String) null, Map.of(), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void enableVideoStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), (String) null, Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable video stream tests")
  public class DisableVideoStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/video", meetingId, sessionId);
    }

    @Test
    @DisplayName("It closes the video stream for the current session and returns a status code 204")
    public void disableVideoStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If video stream is already closed for the current session, correctly it ignores and returns a status code 204")
    public void disableVideoStream_testOkVideoStreamAlreadyCloseWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("It closes the video stream for another session and returns a status code 204")
    public void disableVideoStream_testOkDisableWithAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If video stream is already closed for another session, correctly it ignores and it returns a status code 204")
    public void disableVideoStream_testOkVideoStreamAlreadyCloseWithAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    public void disableVideoStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void disableVideoStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void disableVideoStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher
        .delete(url(UUID.randomUUID(), user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    public void disableVideoStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), "-"), Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable audio stream tests")
  public class EnableAudioStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/audio", meetingId);
    }

    @Test
    @DisplayName("Audio stream correctly opened for the current session and it returns a status code 204")
    public void enableAudioStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(false).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If audio stream is already opened for the current session, correctly it ignores and returns a status code 204")
    public void enableAudioStream_testOkAudioStreamAlreadyOpenWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void enableAudioStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void enableAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher
        .put(url(UUID.randomUUID()), (String) null, Map.of("session-id", user1session1), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the current user doesn't have the session identifier, it returns a status code 400")
    public void enableAudioStream_testErrorCurrentUserWithoutSessionId() throws Exception {
      MockHttpResponse response = dispatcher
        .put(url(UUID.randomUUID()), (String) null, Map.of(), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void enableAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), (String) null, Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable audio stream tests")
  public class DisableAudioStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/audio", meetingId, sessionId);
    }

    @Test
    @DisplayName("It closes the audio stream for the current session and returns a status code 204")
    public void disableAudioStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If audio stream is already closed for the current session, correctly it ignores and returns a status code 204")
    public void disableAudioStream_testOkAudioStreamAlreadyCloseWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("It closes the audio stream for another session and returns a status code 204")
    public void disableAudioStream_testOkDisableWithAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(true).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If audio stream is already closed for another session, correctly it ignores and it returns a status code 204")
    public void disableAudioStream_testOkAudioStreamAlreadyCloseWithAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    public void disableAudioStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void disableAudioStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void disableAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher
        .delete(url(UUID.randomUUID(), user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    public void disableAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), "-"), Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable screen share stream tests")
  public class EnableScreenShareStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/screen", meetingId);
    }

    @Test
    @DisplayName("Screen share stream correctly opened for the current session and it returns a status code 204")
    public void enableScreenShareStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If screen share stream is already opened for the current session, correctly it ignores and returns a status code 204")
    public void enableScreenShareStream_testOkScreenShareStreamAlreadyOpenWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(true)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void enableScreenShareStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(false)));

      MockHttpResponse response = dispatcher
        .put(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void enableScreenShareStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher
        .put(url(UUID.randomUUID()), (String) null, Map.of("session-id", user1session1), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the current user doesn't have the session identifier, it returns a status code 400")
    public void enableScreenShareStream_testErrorCurrentUserWithoutSessionId() throws Exception {
      MockHttpResponse response = dispatcher
        .put(url(UUID.randomUUID()), (String) null, Map.of(), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void enableScreenShareStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), (String) null, Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable screen share stream tests")
  public class DisableScreenShareStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/screen", meetingId, sessionId);
    }

    @Test
    @DisplayName("It closes the screen stream for the current session and returns a status code 204")
    public void disableScreenShareStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If screen share stream is already closed for the current session, correctly it ignores and returns a status code 204")
    public void disableScreenShareStream_testOkScreenShareStreamAlreadyCloseWithSessionEqualToCurrent()
      throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("It closes the screen share stream for another session and returns a status code 204")
    public void disableScreenShareStream_testOkDisableWithAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If screen share stream is already closed for another session, correctly it ignores and it returns a status code 204")
    public void disableScreenShareStream_testOkScreenShareStreamAlreadyCloseWithAnotherSession() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    public void disableScreenShareStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(false)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void disableScreenShareStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(true)));

      MockHttpResponse response = dispatcher
        .delete(url(meetingId, user2session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void disableScreenShareStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher
        .delete(url(UUID.randomUUID(), user1session1), Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    public void disableScreenShareStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID(), "-"), Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

}
