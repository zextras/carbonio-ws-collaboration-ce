package com.zextras.carbonio.chats.meeting.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils.RoomMemberField;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.meeting.api.RoomsApi;
import com.zextras.carbonio.chats.meeting.it.annotations.MeetingApiIntegrationTest;
import com.zextras.carbonio.chats.meeting.it.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.meeting.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import com.zextras.carbonio.chats.meeting.model.ParticipantDto;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.meeting.repository.ParticipantRepository;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@MeetingApiIntegrationTest
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
  private static UUID   user2Id;
  private static UUID   user3Id;
  private static String user1Token;
  private static UUID   room1Id;
  private static UUID   room2Id;
  private static UUID   room3Id;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    user1Token = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    user2Id = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    user3Id = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
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
        ParticipantBuilder.create(user1Id, "user1session1").microphoneOn(true).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(false).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").microphoneOn(false).cameraOn(false)));
      meetingTestUtils.generateAndSaveMeeting(meeting2Id, room2Id, List.of(
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user2session1").microphoneOn(false).cameraOn(true)));

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
      assertEquals("user1session1", participant.get().getSessionId());
      assertTrue(participant.get().isHasCameraOn());
      assertTrue(participant.get().isHasMicrophoneOn());

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
      assertEquals("user2session1", participant.get().getSessionId());
      assertFalse(participant.get().isHasCameraOn());
      assertTrue(participant.get().isHasMicrophoneOn());
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
    @DisplayName("If there isn't an authenticated user then it returns a status code 401")
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
        ParticipantBuilder.create(user1Id, "user1session1").microphoneOn(true).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(false).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").microphoneOn(false).cameraOn(false)));

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
      assertEquals("user1session1", participant1.get().getSessionId());
      assertTrue(participant1.get().isHasCameraOn());
      assertTrue(participant1.get().isHasMicrophoneOn());
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
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(false).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").microphoneOn(false).cameraOn(false)));

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
    @DisplayName("Given a room identifier, if there isn't an authenticated user then it returns a status code 401")
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
        ParticipantBuilder.create(user1Id, "user1session1").microphoneOn(true).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(false).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").microphoneOn(false).cameraOn(false)));

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertEquals(0, participantRepository.getParticipantsByMeetingId(meetingId.toString()).size());
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
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(false).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").microphoneOn(false).cameraOn(false)));

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isPresent());
      assertEquals(3, participantRepository.getParticipantsByMeetingId(meetingId.toString()).size());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void deleteMeetingById_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

    }

    @Test
    @DisplayName("Given a room identifier, if there isn't an authenticated user then it returns a status code 401")
    public void getMeetingById_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

}
