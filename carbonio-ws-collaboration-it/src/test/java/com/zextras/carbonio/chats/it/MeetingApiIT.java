// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.tools.VideoServerMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils.RoomMemberField;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.api.MeetingsApi;
import com.zextras.carbonio.meeting.model.AudioStreamSettingsDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto.TypeEnum;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import com.zextras.carbonio.meeting.model.MeetingUserDto;
import com.zextras.carbonio.meeting.model.NewMeetingDataDto;
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
import org.mockserver.model.Header;
import org.mockserver.verify.VerificationTimes;

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
  private final MongooseImMockServer      mongooseImMockServer;
  private final RoomRepository            roomRepository;
  private final VideoServerMockServer     videoServerMockServer;

  public MeetingApiIT(
    MeetingsApi meetingsApi, ResteasyRequestDispatcher dispatcher,
    MongooseImMockServer mongooseImMockServer,
    MeetingRepository meetingRepository,
    ParticipantRepository participantRepository,
    RoomRepository roomRepository,
    MeetingTestUtils meetingTestUtils,
    ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    UserManagementMockServer userManagementMockServer, Clock clock,
    VideoServerMockServer videoServerMockServer
  ) {
    this.dispatcher = dispatcher;
    this.mongooseImMockServer = mongooseImMockServer;
    this.meetingRepository = meetingRepository;
    this.participantRepository = participantRepository;
    this.roomRepository = roomRepository;
    this.meetingTestUtils = meetingTestUtils;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.userManagementMockServer = userManagementMockServer;
    this.videoServerMockServer = videoServerMockServer;
    this.dispatcher.getRegistry().addSingletonResource(meetingsApi);
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
  @DisplayName("Create meeting tests")
  class CreateMeetingTests {

    private static final String URL = "/meetings";

    @Test
    @DisplayName("Create a meeting from a roomId")
    void createMeetingRoom_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.post(URL,
        objectMapper.writeValueAsString(
          NewMeetingDataDto.create()
            .name("test")
            .meetingType(MeetingTypeDto.PERMANENT)
            .roomId(room1Id)),
        user1Token);
      assertEquals(200, response.getStatus());
      MeetingDto meeting = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(room1Id, meeting.getRoomId());
      assertEquals(false, meeting.isActive());
      assertEquals(MeetingTypeDto.PERMANENT, meeting.getMeetingType());
      assertEquals("test", meeting.getName());
    }

    @Test
    @DisplayName("Create a meeting from a list of Users")
    void createMeetingUsers_testOk() throws Exception {
      mongooseImMockServer
        .when(request().withMethod("POST").withPath("/api/graphql")
          .withHeaders(Header.header("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ="))
        )
        .respond(response()
          .withStatusCode(200)
          .withBody("{ \"data\": { \"mock\": \"success\" } }")
          .withHeaders(Header.header("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ="),
            Header.header("Accept", "application/json"))
        );
      MockHttpResponse response = dispatcher.post(URL,
        objectMapper.writeValueAsString(
          NewMeetingDataDto.create()
            .name("test")
            .meetingType(MeetingTypeDto.SCHEDULED)
            .users(List.of(
              MeetingUserDto.create().userId(user2Id),
              MeetingUserDto.create().userId(user3Id)
            ))
        ),
        user1Token);
      assertEquals(200, response.getStatus());
      MeetingDto meeting = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(false, meeting.isActive());
      assertEquals(MeetingTypeDto.SCHEDULED, meeting.getMeetingType());
      assertEquals("test", meeting.getName());
    }

    @Test
    @DisplayName("Create a meeting Bad Request")
    void createMeeting_testKO() throws Exception {
      MockHttpResponse response = dispatcher.post(URL,
        objectMapper.writeValueAsString(
          NewMeetingDataDto.create()
            .name("test")
            .meetingType(MeetingTypeDto.SCHEDULED)
        ),
        user1Token);
      assertEquals(400, response.getStatus());
    }
  }

  @Nested
  @DisplayName("Update meeting status")
  class UpdateMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s", meetingId);
    }

    @Test
    @DisplayName("Start a meeting")
    void startMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meeting1Id = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));
      videoServerMockServer.mockRequestedResponse("POST", "/janus",
        "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"data\":{\"id\":\"connectionId\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId",
        "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"data\":{\"id\":\"audioHandleId\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"create\",\"room\":\"${json-unit.ignore-element}\",\"permanent\":false,\"description\":\"${json-unit.ignore-element}\",\"sampling_rate\":16000,\"audio_active_packets\":10,\"audio_level_average\":55,\"record\":false,\"private\":false,\"audiolevel_event\":true},\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"audiobridge\":\"created\",\"room\":\"audioRoomId\"}}}",
        true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId",
        "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"data\":{\"id\":\"videoHandleId\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"create\",\"room\":\"${json-unit.ignore-element}\",\"permanent\":false,\"description\":\"${json-unit.ignore-element}\",\"publishers\":100,\"bitrate\":200,\"bitrate_cap\":true,\"record\":false,\"private\":false,\"videocodec\":\"vp8,h264,vp9,h265,av1\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"videoroom\":\"created\",\"room\":\"videoRoomId\"}}}", true);

      MockHttpResponse response = dispatcher.post(url(meeting1Id) + "/start", user1Token);

      assertEquals(200, response.getStatus());
      MeetingDto meeting = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(meeting1Id, meeting.getId());
      assertEquals(true, meeting.isActive());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"create\",\"room\":\"${json-unit.ignore-element}\",\"permanent\":false,\"description\":\"${json-unit.ignore-element}\",\"sampling_rate\":16000,\"audio_active_packets\":10,\"audio_level_average\":55,\"record\":false,\"private\":false,\"audiolevel_event\":true},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"create\",\"room\":\"${json-unit.ignore-element}\",\"permanent\":false,\"description\":\"${json-unit.ignore-element}\",\"publishers\":100,\"bitrate\":200,\"bitrate_cap\":true,\"record\":false,\"private\":false,\"videocodec\":\"vp8,h264,vp9,h265,av1\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Stop a meeting")
    void stopMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meeting1Id = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(
          ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
          ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
          ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
          ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)),
        true,
        null);
      meetingTestUtils.insertVideoServerMeeting(
        meeting1Id.toString(),
        "connectionId",
        "audioHandleId",
        "videoHandleId",
        "audioRoomId",
        "videoRoomId");
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
        true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
        true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}",
        true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}",
        true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId",
        "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}",
        true);
      MockHttpResponse response = dispatcher.post(url(meeting1Id) + "/stop",
        user1Token);
      assertEquals(200, response.getStatus());
      MeetingDto meeting = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(meeting1Id, meeting.getId());
      assertEquals(false, meeting.isActive());

      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }
  }

  @Nested
  @DisplayName("List meetings tests")
  public class ListMeetingTests {

    private static final String URL = "/meetings";

    @Test
    @DisplayName("Correctly gets the meetings of authenticated user")
    public void listMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room2Id.toString()).type(RoomTypeDto.GROUP).name("room2")
          .description("Room two"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room3Id.toString()).type(RoomTypeDto.GROUP).name("room3")
          .description("Room three"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meeting1Id = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));
      UUID meeting2Id = meetingTestUtils.generateAndSaveMeeting(room2Id, List.of(
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
      assertTrue(participant.get().isVideoStreamEnabled());
      assertTrue(participant.get().isAudioStreamEnabled());

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
      assertFalse(participant.get().isVideoStreamEnabled());
      assertTrue(participant.get().isAudioStreamEnabled());
    }

    @Test
    @DisplayName("If rooms, which user is member of, hasn't any meetings, it returns an empty list")
    public void listMeeting_testUserRoomsHasNoMeetings() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room2Id.toString()).type(RoomTypeDto.GROUP).name("room2")
          .description("Room two"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room3Id.toString()).type(RoomTypeDto.GROUP).name("room3")
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
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("room1")
          .description("Room one"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room2Id.toString()).type(RoomTypeDto.GROUP).name("room2")
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
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
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
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void getMeetingById_testUserIsNotRoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
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
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      meetingTestUtils.insertVideoServerMeeting(
        meetingId.toString(),
        "connectionId",
        "audioHandleId",
        "videoHandleId",
        "audioRoomId",
        "videoRoomId");
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId",
        "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertEquals(0, participantRepository.getByMeetingId(meetingId.toString()).size());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void deleteMeetingById_testUserIsNotRoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
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
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));
      meetingTestUtils.insertVideoServerMeeting(
        meetingId.toString(),
        "connectionId",
        "audioHandleId",
        "videoHandleId",
        "audioRoomId",
        "videoRoomId");
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus",
        "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\", \"data\":{\"id\":\"connectionId_user1session1\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connectionId_user1session1",
        "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user1session1\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connectionId_user1session1/handleId_user1session1",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user1session1/video\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connectionId_user1session1/handleId_user1session1",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user1session1/screen\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\", \"data\":{\"id\":\"screenHandleId_user1session1\"}}", true);

      MockHttpResponse response = dispatcher.post(url(meetingId),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(4, meeting.getParticipants().size());
      Participant newParticipant = meeting.getParticipants().stream().filter(participant ->
        user1Id.toString().equals(participant.getUserId()) && user1session1.equals(participant.getQueueId())
      ).findAny().orElseThrow();
      assertTrue(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connectionId_user1session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(2));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connectionId_user1session1/handleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user1session1/video\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connectionId_user1session1/handleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user1session1/screen\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a meeting identifier, the authenticated user correctly joins to the meeting with another session")
    public void joinMeeting_testOkSameUserAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));
      meetingTestUtils.insertVideoServerMeeting(
        meetingId.toString(),
        "connectionId",
        "audioHandleId",
        "videoHandleId",
        "audioRoomId",
        "videoRoomId");
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus",
        "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\", \"data\":{\"id\":\"connectionId_user2session2\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connectionId_user2session2",
        "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user2session2\"}}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connectionId_user2session2/handleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user2session2/video\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connectionId_user2session2/handleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user2session2/screen\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\", \"data\":{\"id\":\"screenHandleId_user2session2\"}}", true);

      MockHttpResponse response = dispatcher.post(url(meetingId),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
        Map.of("session-id", user2session2), user2Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(4, meeting.getParticipants().size());
      Participant newParticipant = meeting.getParticipants().stream().filter(participant ->
        user2Id.toString().equals(participant.getUserId()) && user2session2.equals(participant.getQueueId())
      ).findAny().orElseThrow();
      assertTrue(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connectionId_user2session2",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(2));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connectionId_user2session2/handleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user2session2/video\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connectionId_user2session2/handleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\"user2session2/screen\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user doesn't have an associated room member then it returns a status code 403")
    public void joinMeeting_testUserIsNotRoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user2Id).owner(true),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.post(url(meetingId),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void joinMeeting_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.post(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user isn’t authenticated then it returns a status code 401")
    public void joinMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.post(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
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
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user2session2,
            "connection_" + user2session2,
            null,
            null)
          .audioHandleId("audioHandleId_" + user2session2)
          .videoInHandleId("videoInHandleId_" + user2session2)
          .videoOutHandleId("videoOutHandleId_" + user2session2)
          .screenHandleId("screenHandleId_" + user2session2));
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/audioHandleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/audioHandleId_user2session2",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/videoInHandleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/videoInHandleId_user2session2",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/videoOutHandleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/videoInHandleId_user2session2",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/videoOutHandleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/videoOutHandleId_user2session2",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/screenHandleId_user2session2",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/screenHandleId_user2session2",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2/screenHandleId_user2session2",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"ack\"}", true);
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user2session2",
        "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);

      MockHttpResponse response = dispatcher
        .post(url(meetingId), (String) null, Map.of("session-id", user2session2), user2Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertTrue(meeting.getParticipants().stream().filter(participant ->
        user2Id.toString().equals(participant.getUserId()) &&
          user2session2.equals(participant.getQueueId())
      ).findAny().isEmpty());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/audioHandleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/audioHandleId_user2session2",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/videoInHandleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/videoInHandleId_user2session2",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/videoOutHandleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/videoInHandleId_user2session2",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/videoOutHandleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/videoOutHandleId_user2session2",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/screenHandleId_user2session2",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"leave\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/screenHandleId_user2session2",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2/screenHandleId_user2session2",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user2session2",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a meeting identifier, " +
      "the authenticated user correctly leaves the meeting as last participant and the meeting is closed")
    public void leaveMeeting_testOkLastParticipant() throws Exception {
      Room room = integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      meetingTestUtils.insertVideoServerSession(
        meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId"),
        user1session1,
        "connection_" + user1session1,
        null,
        null);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connection_user1session1",
        "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/videoHandleId",
        "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId",
        "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
        "{\"janus\":\"success\"}", true);

      MockHttpResponse response = dispatcher
        .post(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertNull(integrationTestUtils.getRoomById(room1Id).orElseThrow().getMeetingId());

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connection_user1session1",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/videoHandleId",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a meeting identifier, if the authenticated user isn't a meeting participant then it returns a status code 404")
    public void leaveMeeting_testIsNotMeetingParticipant() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id),
          RoomMemberField.create().id(user3Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session2).audioStreamOn(true).videoStreamOn(false),
        ParticipantBuilder.create(user3Id, user3session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher
        .post(url(meetingId), (String) null, Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the meeting doesn't exist then it returns a status code 404")
    public void leaveMeeting_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.post(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a meeting identifier, if the user isn’t authenticated then it returns a status code 401")
    public void leaveMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.post(url(UUID.randomUUID()),
        objectMapper.writeValueAsString(
          JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
        Map.of("session-id", user1session1), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable video stream tests")
  public class EnableVideoStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/media", meetingId, sessionId);
    }

    @Test
    @DisplayName("Video stream correctly enabled for the current session and it returns a status code 204")
    public void enableVideoStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(false)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            null)
          .videoOutHandleId("videoOutHandleId_" + user1session1));
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user1session1/videoOutHandleId_user1session1",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"publish\"},\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}",
        "{\"janus\":\"ack\"}", true);

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasVideoStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user1session1/videoOutHandleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"publish\"},\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("If video stream is already enabled for the current session, correctly it ignores and returns a status code 204")
    public void enableVideoStream_testOkVideoStreamAlreadyEnabledWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void enableVideoStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void enableVideoStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the current user doesn't have the session identifier, it returns a status code 400")
    public void enableVideoStream_testErrorCurrentUserWithoutSessionId() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
        Map.of(), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void enableVideoStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
        Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable video stream tests")
  public class DisableVideoStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/media", meetingId, sessionId);
    }

    @Test
    @DisplayName("It disables the video stream for the current session and returns a status code 204")
    public void disableVideoStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            null)
          .videoOutStreamOn(true));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If video stream is already disabled for the current session, correctly it ignores and returns a status code 204")
    public void disableVideoStream_testOkVideoStreamAlreadyDisabledWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("It disables the video stream for another session and returns a status code 204")
    public void disableVideoStream_testOkDisableWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(true)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user2session1,
            "connection_" + user2session1,
            null,
            null)
          .videoOutStreamOn(true));
      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If video stream is already disabled for another session, correctly it ignores and it returns a status code 204")
    public void disableVideoStream_testOkVideoStreamAlreadyDisabledWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    public void disableVideoStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void disableVideoStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void disableVideoStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    public void disableVideoStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
        Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable audio stream tests")
  public class EnableAudioStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/audio", meetingId, sessionId);
    }

    @Test
    @DisplayName("Audio stream correctly enabled for the current session and it returns a status code 204")
    public void enableAudioStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(false).videoStreamOn(true)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            null)
          .audioStreamOn(false));
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"unmute\",\"room\":\"audioRoomId\",\"id\":\"user1session1\"},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"audiobridge\":\"success\"}}}", true);

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasAudioStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"unmute\",\"room\":\"audioRoomId\",\"id\":\"user1session1\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("If audio stream is already enabled for the current session, correctly it ignores and returns a status code 204")
    public void enableAudioStream_testOkAudioStreamAlreadyEnabledWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void enableAudioStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void enableAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the current user doesn't have the session identifier, it returns a status code 400")
    public void enableAudioStream_testErrorCurrentUserWithoutSessionId() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
        Map.of(), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void enableAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
        Map.of(), null);

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
    @DisplayName("It disables the audio stream for the current session and returns a status code 204")
    public void disableAudioStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            "screenHandleId_" + user1session1)
          .audioStreamOn(true));
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"mute\",\"room\":\"audioRoomId\",\"id\":\"user1session1\"},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"audiobridge\":\"success\"}}}", true);

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"mute\",\"room\":\"audioRoomId\",\"id\":\"user1session1\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("If audio stream is already disabled for the current session, correctly it ignores and returns a status code 204")
    public void disableAudioStream_testOkAudioStreamAlreadyDisabledWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("It disables the audio stream for another session and returns a status code 204")
    public void disableAudioStream_testOkDisableWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(true).videoStreamOn(false)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            "screenHandleId_" + user1session1)
          .audioStreamOn(true));
      videoServerMockServer.mockRequestedResponse("POST", "/janus/connectionId/audioHandleId",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"mute\",\"room\":\"audioRoomId\",\"id\":\"user1session1\"},\"apisecret\":\"secret\"}",
        "{\"plugindata\":{\"data\":{\"audiobridge\":\"success\"}}}", true);

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST", "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"mute\",\"room\":\"audioRoomId\",\"id\":\"user1session1\"},\"apisecret\":\"secret\"}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("If audio stream is already disabled for another session, correctly it ignores and it returns a status code 204")
    public void disableAudioStream_testOkAudioStreamAlreadyDisabledWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    public void disableAudioStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).videoStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void disableAudioStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void disableAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), user1session1),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    public void disableAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
        Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable screen stream tests")
  public class EnableScreenStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/media", meetingId, sessionId);
    }

    @Test
    @DisplayName("Screen stream correctly enabled for the current session and it returns a status code 204")
    public void enableScreenStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(false)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            "screenHandleId_" + user1session1)
          .screenStreamOn(false));
      videoServerMockServer.mockRequestedResponse("POST",
        "/janus/connection_user1session1/screenHandleId_user1session1",
        "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"publish\"},\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}",
        "{\"janus\":\"ack\"}", true);

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasScreenStreamOn());
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("POST",
          "/janus/connection_user1session1/screenHandleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"publish\"},\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}"),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("If screen stream is already enabled for the current session, correctly it ignores and returns a status code 204")
    public void enableScreenStream_testOkScreenStreamAlreadyEnabledWithSessionEqualToCurrent()
      throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(true)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertTrue(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void enableScreenStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void enableScreenStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
        Map.of("session-id", user1session1), user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the current user doesn't have the session identifier, it returns a status code 400")
    public void enableScreenStream_testErrorCurrentUserWithoutSessionId() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
        Map.of(), user1Token);
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    public void enableScreenStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
        Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable screen stream tests")
  public class DisableScreenStreamTests {

    private String url(UUID meetingId, String sessionId) {
      return String.format("/meetings/%s/sessions/%s/media", meetingId, sessionId);
    }

    @Test
    @DisplayName("It disables the screen stream for the current session and returns a status code 204")
    public void disableScreenStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(true)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user1session1,
            "connection_" + user1session1,
            null,
            "screenHandleId_" + user1session1)
          .screenStreamOn(true));
      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If screen stream is already disabled for the current session, correctly it ignores and returns a status code 204")
    public void disableScreenStream_testOkScreenStreamAlreadyDisabledWithSessionEqualToCurrent()
      throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user1session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("It disables the screen stream for another session and returns a status code 204")
    public void disableScreenStream_testOkDisableWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(true)));
      meetingTestUtils.updateVideoServerSession(
        meetingTestUtils.insertVideoServerSession(
            meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
            user2session1,
            "connection_" + user2session1,
            null,
            "screenHandleId_" + user2session1)
          .screenStreamOn(true));
      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If screen stream is already disabled for another session, correctly it ignores and it returns a status code 204")
    public void disableScreenStream_testOkScreenStreamAlreadyDisabledWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id).owner(true),
          RoomMemberField.create().id(user2Id)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant = meetingTestUtils.getParticipant(meetingId, user2session1).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    public void disableScreenStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(
          RoomMemberField.create().id(user1Id),
          RoomMemberField.create().id(user2Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id, List.of(
        ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).videoStreamOn(true),
        ParticipantBuilder.create(user2Id, user2session1).audioStreamOn(false).screenStreamOn(false)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested session isn't in the meeting participants, it returns a status code 404")
    public void disableScreenStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
        Room.create().id(room1Id.toString()).type(RoomTypeDto.GROUP).name("name").description("description"),
        List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId = meetingTestUtils.generateAndSaveMeeting(room1Id,
        List.of(ParticipantBuilder.create(user1Id, user1session1).audioStreamOn(true).screenStreamOn(true)));

      MockHttpResponse response = dispatcher.put(
        url(meetingId, user2session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    public void disableScreenStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), user1session1),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of("session-id", user1session1), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    public void disableScreenStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.put(
        url(UUID.randomUUID(), UUID.randomUUID().toString()),
        objectMapper.writeValueAsString(MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
        Map.of(), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

}
