package com.zextras.carbonio.chats.meeting.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.data.entity.Participant;
import com.zextras.carbonio.chats.meeting.it.annotations.MeetingApiIntegrationTest;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@MeetingApiIntegrationTest
public class RoomsApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final MeetingRepository         meetingRepository;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final UserManagementMockServer  userManagementMockServer;
  private final AppClock                  clock;

  public RoomsApiIT(
    RoomsApi roomsApi, ResteasyRequestDispatcher dispatcher,
    MeetingRepository meetingRepository, ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    UserManagementMockServer userManagementMockServer, Clock clock
  ) {
    this.dispatcher = dispatcher;
    this.meetingRepository = meetingRepository;
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
  @DisplayName("Create meeting by room tests")
  public class CreateMeetingByRoomTests {

    private String url(UUID roomId) {
      return String.format("/rooms/%s/meeting", roomId);
    }

    @Test
    @DisplayName("Given a room identifier, it creates a new meeting associated with the indicated room and returns its data")
    public void createMeetingByRoomTest_testOk() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
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
        response = dispatcher.put(url(roomId), null, user1Token);
      }
      clock.removeFixTime();
      assertEquals(201, response.getStatus());
      MeetingDto meetingDto = objectMapper.readValue(response.getContentAsString(), MeetingDto.class);
      assertNotNull(meetingDto);
      assertEquals("86cc37de-1217-4056-8c95-69997a6bccce", meetingDto.getId().toString());
      assertEquals(roomId, meetingDto.getRoomId());
      assertNotNull(meetingDto.getParticipants());
      assertEquals(0, meetingDto.getParticipants().size());
      assertEquals(executionInstant, meetingDto.getCreatedAt().toInstant());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if there is a meeting associated with the room then it returns a status code 409")
    public void createMeetingByRoomTest_testErrorMeetingExists() throws Exception {
      UUID meetingId = UUID.fromString("86cc37de-1217-4056-8c95-69997a6bccce");
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      generateAndSaveMeeting(meetingId, roomId, List.of(user1Id, user2Id, user3Id));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user1Token);

      assertEquals(409, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the room doesn't exists then it returns a status code 404")
    public void createMeetingByRoomTest_testErrorRoomNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(url(roomId), null, user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then return a status code 403")
    public void createMeetingByRoomTest_testErrorUserIsNotARoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(roomId.toString()).type(RoomTypeDto.GROUP).hash("-").name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.put(url(roomId), null, user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }


    @Test
    @DisplayName("Given a room identifier, if there isn't an authenticated user then it returns a status code 401")
    public void createMeetingByRoomTest_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(url(roomId), null, null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

  }

  private Meeting generateAndSaveMeeting(UUID meetingId, UUID roomId, List<UUID> usersIds) {
    List<Participant> participants = usersIds.stream().map(userId ->
        Participant.create(userId.toString(), meetingId.toString(), UUID.randomUUID().toString()))
      .collect(Collectors.toList());
    return meetingRepository.insert(Meeting.create()
      .id(meetingId.toString())
      .roomId(roomId.toString())
      .participants(participants));
  }

}
