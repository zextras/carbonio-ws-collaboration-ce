package com.zextras.carbonio.chats.meeting.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  private static UUID   roomId;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    user1Token = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    user2Id = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    user3Id = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
    roomId = UUID.fromString("26c15cd7-619d-4cbd-a221-486efb1bfc9d");
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
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
        ParticipantBuilder.create(user1Id, "user1session1").microphoneOn(true).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session1").microphoneOn(false).cameraOn(true),
        ParticipantBuilder.create(user2Id, "user2session2").microphoneOn(true).cameraOn(false),
        ParticipantBuilder.create(user3Id, "user3session1").microphoneOn(false).cameraOn(false)));

      MockHttpResponse response = dispatcher.get(url(meetingId), user1Token);

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
      assertTrue(participant1.get().isHasCameraOn());
      assertTrue(participant1.get().isHasMicrophoneOn());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void getMeetingById_testUserIsNotRoomMember() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
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
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
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
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      meetingTestUtils.generateAndSaveMeeting(meetingId, roomId, List.of(
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
