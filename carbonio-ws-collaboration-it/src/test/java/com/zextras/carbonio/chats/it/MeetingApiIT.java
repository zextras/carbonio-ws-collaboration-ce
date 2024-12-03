// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.entity.*;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request.VideoRecorderRequest;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.tools.ConsulMockServer;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.VideoRecorderMockServer;
import com.zextras.carbonio.chats.it.tools.VideoServerMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils.RoomMemberField;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.api.MeetingsApi;
import com.zextras.carbonio.meeting.model.*;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto.TypeEnum;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import com.zextras.carbonio.meeting.model.NewMeetingDataDto;
import com.zextras.carbonio.meeting.model.ParticipantDto;
import com.zextras.carbonio.meeting.model.SessionDescriptionProtocolDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class MeetingApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ParticipantRepository participantRepository;
  private final WaitingParticipantRepository waitingParticipantRepository;
  private final MeetingTestUtils meetingTestUtils;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MongooseImMockServer mongooseImMockServer;
  private final VideoServerMockServer videoServerMockServer;
  private final VideoRecorderMockServer videoRecorderMockServer;
  private final ConsulMockServer consulMockServer;
  private final AppClock clock;

  public MeetingApiIT(
      MeetingsApi meetingsApi,
      ResteasyRequestDispatcher dispatcher,
      MongooseImMockServer mongooseImMockServer,
      ParticipantRepository participantRepository,
      WaitingParticipantRepository waitingParticipantRepository,
      MeetingTestUtils meetingTestUtils,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      VideoServerMockServer videoServerMockServer,
      VideoRecorderMockServer videoRecorderMockServer,
      ConsulMockServer consulMockServer,
      Clock clock) {
    this.dispatcher = dispatcher;
    this.mongooseImMockServer = mongooseImMockServer;
    this.participantRepository = participantRepository;
    this.waitingParticipantRepository = waitingParticipantRepository;
    this.meetingTestUtils = meetingTestUtils;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.videoServerMockServer = videoServerMockServer;
    this.videoRecorderMockServer = videoRecorderMockServer;
    this.consulMockServer = consulMockServer;
    this.dispatcher.getRegistry().addSingletonResource(meetingsApi);
    this.clock = (AppClock) clock;
  }

  private static UUID user1Id;
  private static String user1Token;
  private static String user1Queue;
  private static UUID user2Id;
  private static String user2Token;
  private static String user2Queue;
  private static UUID user3Id;
  private static String user3Queue;
  private static UUID room1Id;
  private static UUID room2Id;
  private static UUID room3Id;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    user1Token = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    user1Queue = UUID.randomUUID().toString();
    user2Id = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    user2Token = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
    user2Queue = UUID.randomUUID().toString();
    user3Id = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
    user3Queue = UUID.randomUUID().toString();
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
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));

      MockHttpResponse response =
          dispatcher.post(
              URL,
              objectMapper.writeValueAsString(
                  NewMeetingDataDto.create()
                      .name("test")
                      .meetingType(MeetingTypeDto.PERMANENT)
                      .roomId(room1Id)),
              user1Token);
      assertEquals(200, response.getStatus());
      MeetingDto meeting =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(room1Id, meeting.getRoomId());
      assertEquals(false, meeting.isActive());
      assertEquals(MeetingTypeDto.PERMANENT, meeting.getMeetingType());
      assertEquals("test", meeting.getName());
    }

    @Test
    @DisplayName("Fail to create meeting if already present")
    void createMeetingRoom_testKO() throws Exception {

      Room room =
          integrationTestUtils.generateAndSaveRoom(
              Room.create()
                  .id(room1Id.toString())
                  .type(RoomTypeDto.GROUP)
                  .name("room1")
                  .description("Room one"),
              List.of(
                  RoomMemberField.create().id(user1Id).owner(true),
                  RoomMemberField.create().id(user2Id),
                  RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      integrationTestUtils.updateRoom(room.meetingId(meeting1Id.toString()));
      MockHttpResponse response =
          dispatcher.post(
              URL,
              objectMapper.writeValueAsString(
                  NewMeetingDataDto.create()
                      .name("test")
                      .meetingType(MeetingTypeDto.PERMANENT)
                      .roomId(room1Id)),
              user1Token);
      assertEquals(409, response.getStatus());
    }

    @Test
    @DisplayName("Create a meeting Bad Request")
    void createMeeting_testKO() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              URL,
              objectMapper.writeValueAsString(
                  NewMeetingDataDto.create().name("test").meetingType(MeetingTypeDto.SCHEDULED)),
              user1Token);
      assertEquals(400, response.getStatus());
    }
  }

  @Nested
  @DisplayName("Start meeting tests")
  class StartMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/start", meetingId);
    }

    @BeforeEach
    void init() {
      UUID serverId = UUID.randomUUID();
      consulMockServer.mockConsulLeaderResponse();
      consulMockServer.mockResponseForVideoserver(serverId);
    }

    @Test
    @DisplayName("Start a meeting")
    void startMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"data\":{\"id\":\"connectionId\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"data\":{\"id\":\"audioHandleId\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"create\","
              + "\"room\":\"${json-unit.ignore-element}\",\"permanent\":false,\"description\":\"${json-unit.ignore-element}\","
              + "\"sampling_rate\":16000,\"audio_active_packets\":10,\"audio_level_average\":65,\"record\":false,\"is_private\":false,\"audiolevel_event\":true},\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"audiobridge\":\"created\",\"room\":\"audioRoomId\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"data\":{\"id\":\"videoHandleId\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"create\","
              + "\"room\":\"${json-unit.ignore-element}\",\"permanent\":false,\"description\":\"${json-unit.ignore-element}\","
              + "\"publishers\":100,\"bitrate\":409600,\"bitrate_cap\":true,\"record\":false,\"is_private\":false,\"videocodec\":\"vp8,h264,vp9,h265,av1\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"videoroom\":\"created\",\"room\":\"videoRoomId\"}}}",
          true);

      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);

      assertEquals(200, response.getStatus());
      MeetingDto meeting =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(meeting1Id, meeting.getId());
      assertEquals(true, meeting.isActive());
    }

    @Test
    @DisplayName("Start a meeting that is already started")
    void startMeeting_testErrorVideoServerMeetingIsAlreadyActive() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      meetingTestUtils.insertVideoServerMeeting(
          meeting1Id.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");

      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);

      assertEquals(424, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Stop meeting tests")
  class StopMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/stop", meetingId);
    }

    @BeforeEach
    void init() {
      UUID serverId = UUID.randomUUID();
      consulMockServer.mockConsulLeaderResponse();
      consulMockServer.mockResponseForVideoserver(serverId);
    }

    @Test
    @DisplayName("Stop a meeting")
    void stopMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of());
      meetingTestUtils.insertVideoServerMeeting(
          meeting1Id.toString(),
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
      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);
      assertEquals(200, response.getStatus());
      MeetingDto meeting =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(meeting1Id, meeting.getId());
      assertEquals(false, meeting.isActive());
    }

    @Test
    @DisplayName("Stop a meeting which is being recorded")
    void stopMeeting_testOkWithRecording() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))
                      .token(user1Token)));
      meetingTestUtils.insertVideoServerMeeting(
          meeting1Id.toString(),
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
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_mjrs\",\"room\":\"audioRoomId\","
              + "\"mjrs\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_recording\",\"room\":\"videoRoomId\","
              + "\"record\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoRecorderMockServer.mockRequestedResponse(
          "POST",
          "/PostProcessor/meeting_" + meeting1Id,
          objectMapper.writeValueAsString(
              VideoRecorderRequest.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName("Test Meeting for " + room1Id.toString())
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .authToken("ZM_AUTH_TOKEN=" + user1Token)
                  .folderId("rec-folder-id")
                  .recordingName("rec-name")),
          true);
      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);
      assertEquals(200, response.getStatus());
      MeetingDto meeting =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(meeting1Id, meeting.getId());
      assertEquals(false, meeting.isActive());
    }
  }

  @Nested
  @DisplayName("List meetings tests")
  class ListMeetingTests {

    private static final String URL = "/meetings";

    @Test
    @DisplayName("Correctly gets the meetings of authenticated user")
    void listMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room2Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room2")
              .description("Room two"),
          List.of(
              RoomMemberField.create().id(user1Id),
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room3Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room3")
              .description("Room three"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      UUID meeting2Id =
          meetingTestUtils.generateAndSaveMeeting(
              room2Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(false),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true)));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<MeetingDto> meetings =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertNotNull(meetings);
      assertEquals(2, meetings.size());
      MeetingDto meeting1Dto =
          meetings.stream().filter(m -> m.getId().equals(meeting1Id)).findAny().orElseThrow();
      assertEquals(meeting1Id, meeting1Dto.getId());
      assertEquals(room1Id, meeting1Dto.getRoomId());
      assertNotNull(meeting1Dto.getParticipants());
      assertEquals(3, meeting1Dto.getParticipants().size());
      assertEquals(
          1,
          (int)
              meeting1Dto.getParticipants().stream()
                  .filter(p -> user1Id.equals(p.getUserId()))
                  .count());
      assertEquals(
          1,
          (int)
              meeting1Dto.getParticipants().stream()
                  .filter(p -> user2Id.equals(p.getUserId()))
                  .count());
      assertEquals(
          1,
          (int)
              meeting1Dto.getParticipants().stream()
                  .filter(p -> user3Id.equals(p.getUserId()))
                  .count());
      Optional<ParticipantDto> participant =
          meeting1Dto.getParticipants().stream()
              .filter(p -> user1Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant.isPresent());
      assertEquals(user1Id, participant.get().getUserId());
      assertTrue(participant.get().isVideoStreamEnabled());
      assertTrue(participant.get().isAudioStreamEnabled());

      MeetingDto meeting2Dto =
          meetings.stream().filter(m -> m.getId().equals(meeting2Id)).findAny().orElseThrow();
      assertEquals(meeting2Id, meeting2Dto.getId());
      assertEquals(room2Id, meeting2Dto.getRoomId());
      assertNotNull(meeting2Dto.getParticipants());
      assertEquals(2, meeting2Dto.getParticipants().size());
      assertEquals(
          1,
          (int)
              meeting2Dto.getParticipants().stream()
                  .filter(p -> user2Id.equals(p.getUserId()))
                  .count());
      assertEquals(
          1,
          (int)
              meeting2Dto.getParticipants().stream()
                  .filter(p -> user3Id.equals(p.getUserId()))
                  .count());
      participant =
          meeting2Dto.getParticipants().stream()
              .filter(p -> user2Id.equals(p.getUserId()))
              .findAny();
      assertTrue(participant.isPresent());
      assertEquals(user2Id, participant.get().getUserId());
      assertFalse(participant.get().isVideoStreamEnabled());
      assertTrue(participant.get().isAudioStreamEnabled());
    }

    @Test
    @DisplayName("If rooms, which user is member of, hasn't any meetings, it returns an empty list")
    void listMeeting_testUserRoomsHasNoMeetings() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room2Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room2")
              .description("Room two"),
          List.of(
              RoomMemberField.create().id(user1Id),
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room3Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room3")
              .description("Room three"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<MeetingDto> meetings =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertNotNull(meetings);
      assertEquals(0, meetings.size());
    }

    @Test
    @DisplayName("If the user is not a member of any room, correctly gets an empty list")
    void listMeeting_testUserHasNotRooms() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room2Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room2")
              .description("Room two"),
          List.of(
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id).owner(true)));

      MockHttpResponse response = dispatcher.get(URL, user1Token);
      assertEquals(200, response.getStatus());
      List<MeetingDto> meetings =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertNotNull(meetings);
      assertEquals(0, meetings.size());
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    void listMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(URL, null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Get meeting by id tests")
  class GetMeetingByIdTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s", meetingId);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, correctly returns the meeting information with participants")
    void getMeetingById_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response = dispatcher.get(url(meetingId), user1Token);

      assertEquals(200, response.getStatus());
      MeetingDto meetingDto =
          objectMapper.readValue(response.getContentAsString(), MeetingDto.class);
      assertNotNull(meetingDto);
      assertEquals(meetingId, meetingDto.getId());
      assertEquals(room1Id, meetingDto.getRoomId());
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
      assertTrue(participant1.get().isVideoStreamEnabled());
      assertTrue(participant1.get().isAudioStreamEnabled());
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the user doesn't have an associated room member then it"
            + " returns a status code 403")
    void getMeetingById_testUserIsNotRoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response = dispatcher.get(url(meetingId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the meeting doesn't exist then it returns a status code"
            + " 404")
    void getMeetingById_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the user isn’t authenticated then it returns a status code"
            + " 401")
    void getMeetingById_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Delete meeting by id tests")
  class DeleteMeetingByIdTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s", meetingId);
    }

    @Test
    @DisplayName("Given a meeting identifier, correctly deletes the meeting and the participants")
    void deleteMeetingById_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connection1Id",
          "audioHandle1Id",
          "videoHandle1Id",
          "audioRoom1Id",
          "videoRoom1Id");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection1Id/videoHandle1Id",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoom1Id\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection1Id/audioHandle1Id",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoom1Id\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection1Id/audioHandle1Id",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection1Id/videoHandle1Id",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection1Id",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertEquals(0, participantRepository.getByMeetingId(meetingId.toString()).size());
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, correctly deletes the meeting and stop the ongoing recording")
    void deleteMeetingById_testOkWithRecording() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room2Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room2Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))
                      .token(user1Token)));
      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connection2Id",
          "audioHandle2Id",
          "videoHandle2Id",
          "audioRoom2Id",
          "videoRoom2Id");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id" + "/audioHandle2Id",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_mjrs\",\"room\":\"audioRoom2Id\","
              + "\"mjrs\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id" + "/videoHandle2Id",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_recording\",\"room\":\"videoRoom2Id\","
              + "\"record\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoRecorderMockServer.mockRequestedResponse(
          "POST",
          "/PostProcessor/meeting_" + meetingId,
          objectMapper.writeValueAsString(
              VideoRecorderRequest.create()
                  .meetingId(meetingId.toString())
                  .meetingName("Test Meeting for " + room2Id.toString())
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .authToken("ZM_AUTH_TOKEN=" + user1Token)),
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id/videoHandle2Id",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoom2Id\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id/audioHandle2Id",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoom2Id\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id/audioHandle2Id",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id/videoHandle2Id",
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection2Id",
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      assertTrue(meetingTestUtils.getMeetingById(meetingId).isEmpty());
      assertEquals(0, participantRepository.getByMeetingId(meetingId.toString()).size());
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the user doesn't have an associated room member then it"
            + " returns a status code 403")
    void deleteMeetingById_testUserIsNotRoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response = dispatcher.delete(url(meetingId), user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);

      assertTrue(meetingTestUtils.getMeetingById(meetingId).isPresent());
      assertEquals(2, participantRepository.getByMeetingId(meetingId.toString()).size());
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the meeting doesn't exist then it returns a status code"
            + " 404")
    void deleteMeetingById_testMeetingNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if the user isn’t authenticated then it returns a status code"
            + " 401")
    void getMeetingById_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Join permanent meeting Tests")
  class JoinPermanentMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/join", meetingId);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, the authenticated user correctly joins to the meeting")
    void joinMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"connectionId_user1session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user1session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user1session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1/handleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\""
              + user1Id
              + "/video\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1/handleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\""
              + user1Id
              + "/screen\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\", \"data\":{\"id\":\"screenHandleId_user1session1\"}}",
          true);

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(200, response.getStatus());
      assertEquals(
          "{\"status\":\"ACCEPTED\"}", new String(response.getOutput(), StandardCharsets.UTF_8));

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(3, meeting.getParticipants().size());
      Participant newParticipant =
          meeting.getParticipants().stream()
              .filter(
                  participant ->
                      user1Id.toString().equals(participant.getUserId())
                          && user1Queue.equals(participant.getQueueId()))
              .findAny()
              .orElseThrow();
      assertFalse(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the user doesn't have an associated room member then it"
            + " returns a status code 403")
    void joinMeeting_testUserIsNotRoomMember() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user2Id).owner(true),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the meeting doesn't exist then it returns a status code"
            + " 404")
    void joinMeeting_testMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the queue id is not present in the request it returns a"
            + " status code 400")
    void joinMeeting_testErrorQueueIdIsNotPresent() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of(),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the user isn’t authenticated then it returns a status code"
            + " 401")
    void joinMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Join scheduled meeting Tests")
  class JoinScheduledMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/join", meetingId);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, the authenticated moderator correctly joins to the meeting")
    void joinMeeting_moderatorTestOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.TEMPORARY)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));

      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id, MeetingType.SCHEDULED, Collections.emptyList(), true, List.of());
      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"connectionId_user1session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user1session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user1session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1/handleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\""
              + user1Id
              + "/video\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user1session1/handleId_user1session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\""
              + user1Id
              + "/screen\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\", \"data\":{\"id\":\"screenHandleId_user1session1\"}}",
          true);

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(200, response.getStatus());
      assertEquals(
          "{\"status\":\"ACCEPTED\"}", new String(response.getOutput(), StandardCharsets.UTF_8));

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(1, meeting.getParticipants().size());
      Participant newParticipant =
          meeting.getParticipants().stream()
              .filter(
                  participant ->
                      user1Id.toString().equals(participant.getUserId())
                          && user1Queue.equals(participant.getQueueId()))
              .findAny()
              .orElseThrow();
      assertFalse(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("Given a meeting identifier, the authenticated is put on queue")
    void joinMeeting_userWaitingTestOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.TEMPORARY)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));

      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id, MeetingType.SCHEDULED, Collections.emptyList(), true, List.of());
      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"connectionId_user1session1\"}}",
          true);

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user2Queue),
              user2Token);
      assertEquals(200, response.getStatus());
      assertEquals(
          "{\"status\":\"WAITING\"}", new String(response.getOutput(), StandardCharsets.UTF_8));

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(0, meeting.getParticipants().size());

      Optional<WaitingParticipant> wp =
          waitingParticipantRepository.getWaitingParticipant(
              meetingId.toString(), user2Id.toString());
      assertTrue(wp.isPresent());
      assertEquals(JoinStatus.WAITING, wp.get().getStatus());
    }

    @Test
    @DisplayName("Given a meeting identifier, the accepted user enters the meeting")
    void joinMeeting_userAcceptedTestOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.TEMPORARY)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));

      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id, MeetingType.SCHEDULED, Collections.emptyList(), true, List.of());
      waitingParticipantRepository.insert(
          WaitingParticipant.create()
              .id(UUID.randomUUID().toString())
              .meetingId(meetingId.toString())
              .userId(user2Id.toString())
              .queueId(user2Queue)
              .status(JoinStatus.ACCEPTED));
      meetingTestUtils.insertVideoServerMeeting(
          meetingId.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus",
          "{\"janus\":\"create\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"connectionId_user2session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user2session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user2session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user2session1",
          "{\"janus\":\"attach\",\"transaction\":\"${json-unit.ignore-element}\",\"plugin\":\"janus.plugin.videoroom\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\", \"data\":{\"id\":\"handleId_user2session1\"}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user2session1/handleId_user2session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\""
              + user2Id
              + "/video\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId_user2session1/handleId_user2session1",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"join\",\"ptype\":\"publisher\",\"room\":\"videoRoomId\",\"id\":\""
              + user2Id
              + "/screen\"},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\", \"data\":{\"id\":\"screenHandleId_user2session1\"}}",
          true);

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user2Queue),
              user2Token);
      assertEquals(200, response.getStatus());
      assertEquals(
          "{\"status\":\"ACCEPTED\"}", new String(response.getOutput(), StandardCharsets.UTF_8));

      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertNotNull(meeting);
      assertEquals(meetingId.toString(), meeting.getId());
      assertEquals(room1Id.toString(), meeting.getRoomId());
      assertEquals(1, meeting.getParticipants().size());
      Participant newParticipant =
          meeting.getParticipants().stream()
              .filter(
                  participant ->
                      user2Id.toString().equals(participant.getUserId())
                          && user2Queue.equals(participant.getQueueId()))
              .findAny()
              .orElseThrow();
      assertFalse(newParticipant.hasAudioStreamOn());
      assertFalse(newParticipant.hasVideoStreamOn());
    }
  }

  @Nested
  @DisplayName("Leave meeting tests")
  class LeaveMeetingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/leave", meetingId);
    }

    @Test
    @DisplayName("Given a meeting identifier, the authenticated user correctly leaves the meeting")
    void leaveMeeting_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));
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

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId), (String) null, Map.of("queue-id", user2Queue), user2Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Meeting meeting = meetingTestUtils.getMeetingById(meetingId).orElseThrow();
      assertTrue(
          meeting.getParticipants().stream()
              .filter(
                  participant ->
                      user2Id.toString().equals(participant.getUserId())
                          && user2Queue.equals(participant.getQueueId()))
              .findAny()
              .isEmpty());
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, the authenticated user correctly leaves the meeting as last"
            + " participant and the meeting is stopped")
    void leaveMeeting_testOkLastParticipant() throws Exception {
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              Room.create()
                  .id(room1Id.toString())
                  .type(RoomTypeDto.GROUP)
                  .name("name")
                  .description("description"),
              List.of(
                  RoomMemberField.create().id(user1Id).owner(true),
                  RoomMemberField.create().id(user2Id),
                  RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)),
              true,
              List.of());
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      meetingTestUtils.insertVideoServerSession(
          meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
          user1Id.toString(),
          user1Queue,
          "connection_" + user1Queue,
          "audioHandleId_" + user1Queue,
          "videoOutHandleId_" + user1Queue,
          "videoInHandleId_" + user1Queue,
          "screenHandleId_" + user1Queue);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/audioHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoOutHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/screenHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue,
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
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

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId), (String) null, Map.of("queue-id", user1Queue), user1Token);

      Optional<Meeting> meetingById = meetingTestUtils.getMeetingById(meetingId);
      assertTrue(meetingById.isPresent());
      assertFalse(meetingById.get().getActive());

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, the authenticated user correctly leaves the meeting as last"
            + " participant and the meeting and recording are both stopped")
    void leaveMeeting_testOkLastParticipantWithRecording() throws Exception {
      Room room =
          integrationTestUtils.generateAndSaveRoom(
              Room.create()
                  .id(room1Id.toString())
                  .type(RoomTypeDto.GROUP)
                  .name("name")
                  .description("description"),
              List.of(
                  RoomMemberField.create().id(user1Id).owner(true),
                  RoomMemberField.create().id(user2Id),
                  RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)),
              true,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))
                      .token(user1Token)));
      integrationTestUtils.updateRoom(room.meetingId(meetingId.toString()));
      meetingTestUtils.insertVideoServerSession(
          meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
          user1Id.toString(),
          user1Queue,
          "connection_" + user1Queue,
          "audioHandleId_" + user1Queue,
          "videoOutHandleId_" + user1Queue,
          "videoInHandleId_" + user1Queue,
          "screenHandleId_" + user1Queue);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/audioHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoOutHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/screenHandleId_" + user1Queue,
          "{\"janus\":\"detach\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue,
          "{\"janus\":\"destroy\",\"transaction\":\"${json-unit.ignore-element}\",\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"videoRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"videoroom\":\"destroyed\"}}}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"destroy\",\"room\":\"audioRoomId\",\"permanent\":false},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"destroyed\"}}}",
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
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_mjrs\",\"room\":\"audioRoomId\","
              + "\"mjrs\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_recording\",\"room\":\"videoRoomId\","
              + "\"record\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoRecorderMockServer.mockRequestedResponse(
          "POST",
          "/PostProcessor/meeting_" + meetingId,
          objectMapper.writeValueAsString(
              VideoRecorderRequest.create()
                  .meetingId(meetingId.toString())
                  .meetingName("Test Meeting for " + room1Id.toString())
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .authToken("ZM_AUTH_TOKEN=" + user1Token)
                  .folderId("rec-folder-id")
                  .recordingName("rec-name")),
          true);

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId), (String) null, Map.of("queue-id", user1Queue), user1Token);

      Optional<Meeting> meetingById = meetingTestUtils.getMeetingById(meetingId);
      assertTrue(meetingById.isPresent());
      assertFalse(meetingById.get().getActive());

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the authenticated user isn't a meeting participant then it"
            + " returns 200")
    void leaveMeeting_testIsNotMeetingParticipant() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.post(
              url(meetingId), (String) null, Map.of("queue-id", user1Queue), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the meeting doesn't exist then it returns a status code"
            + " 404")
    void leaveMeeting_testMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given a meeting identifier, if the user isn’t authenticated then it returns a status code"
            + " 401")
    void leaveMeeting_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  JoinSettingsDto.create().audioStreamEnabled(true).videoStreamEnabled(false)),
              Map.of("queue-id", user1Queue),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Queue tests")
  class QueueTests {
    private String url(UUID meetingId) {
      return String.format("/meetings/%s/queue", meetingId);
    }

    @Test
    @DisplayName("Gets the list of queued users")
    void getQueuedUsers_moderatorTestOk()
        throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.TEMPORARY)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(false)));
      waitingParticipantRepository.insert(
          WaitingParticipant.create()
              .id(UUID.randomUUID().toString())
              .meetingId(meetingId.toString())
              .userId(user2Id.toString())
              .queueId(user2Queue)
              .status(JoinStatus.WAITING));
      waitingParticipantRepository.insert(
          WaitingParticipant.create()
              .id(UUID.randomUUID().toString())
              .meetingId(meetingId.toString())
              .userId(user3Id.toString())
              .queueId(user3Queue)
              .status(JoinStatus.WAITING));

      MockHttpResponse response = dispatcher.get(url(meetingId), user1Token);
      assertEquals(200, response.getStatus());

      QueuedUsersDto queuedUsers =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(2, queuedUsers.getUsers().size());
      assertEquals(List.of(user2Id, user3Id), queuedUsers.getUsers());
    }

    @Test
    @DisplayName("Approve an user in waiting list")
    void approveUser_testOk() throws JsonProcessingException, URISyntaxException {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.TEMPORARY)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      mongooseImMockServer.mockAddRoomMember(
          room1Id.toString(), user1Id.toString(), user2Id.toString(), true);
      String hopedXmppAffiliationMessage1 =
          String.format(
                  "<message xmlns='jabber:client' from='%s@carbonio' to='%s@muclight.carbonio'"
                      + " type='groupchat'>",
                  user1Id, room1Id)
              + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
              + "<operation>memberAdded</operation>"
              + String.format("<user-id>%s</user-id>", user2Id)
              + "</x>"
              + "<body/>"
              + "</message>";
      mongooseImMockServer.mockSendStanza(hopedXmppAffiliationMessage1, true);
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(false)));
      waitingParticipantRepository.insert(
          WaitingParticipant.create()
              .id(UUID.randomUUID().toString())
              .meetingId(meetingId.toString())
              .userId(user2Id.toString())
              .queueId(user2Queue)
              .status(JoinStatus.WAITING));
      MockHttpResponse response =
          dispatcher.post(
              url(meetingId) + "/" + user2Id,
              objectMapper.writeValueAsString(
                  QueuedUserUpdateDto.create().status(QueueUpdateStatusDto.ACCEPTED)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      Optional<WaitingParticipant> wp1 =
          waitingParticipantRepository.getWaitingParticipant(
              meetingId.toString(), user2Id.toString());
      assertTrue(wp1.isPresent());
      WaitingParticipant wp = wp1.get();
      assertEquals(JoinStatus.ACCEPTED, wp.getStatus());
    }
  }

  @Nested
  @DisplayName("Enable video stream tests")
  class EnableVideoStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/media", meetingId);
    }

    @Test
    @DisplayName(
        "Video stream correctly enabled for the current session and it returns a status code 204")
    void enableVideoStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(false)));
      meetingTestUtils.insertVideoServerSession(
          meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId"),
          user1Id.toString(),
          user1Queue,
          "connection_" + user1Queue,
          "audioHandleId_" + user1Queue,
          "videoOutHandleId_" + user1Queue,
          "videoInHandleId_" + user1Queue,
          "screenHandleId_" + user1Queue);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoOutHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"publish\"},\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertTrue(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName(
        "If video stream is already enabled for the current session, correctly it ignores and"
            + " returns a status code 204")
    void enableVideoStream_testOkVideoStreamAlreadyEnabledWithSessionEqualToCurrent()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertTrue(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName(
        "If the requested session isn't in the meeting participants, it returns a status code 404")
    void enableVideoStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void enableVideoStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If sdp is not present then it returns a status code 400")
    void enableVideoStream_testErrorSdpNotPresent() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    void enableVideoStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("sdp")),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable video stream tests")
  class DisableVideoStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/media", meetingId);
    }

    @Test
    @DisplayName(
        "It disables the video stream for the current session and returns a status code 204")
    void disableVideoStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
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
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  null,
                  null,
                  null,
                  null)
              .videoOutStreamOn(true));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName(
        "If video stream is already disabled for the current session, correctly it ignores and"
            + " returns a status code 204")
    void disableVideoStream_testOkVideoStreamAlreadyDisabledWithSessionEqualToCurrent()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertFalse(participant.hasVideoStreamOn());
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void disableVideoStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    void disableVideoStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false)),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable audio stream tests")
  class EnableAudioStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/audio", meetingId);
    }

    @Test
    @DisplayName(
        "Audio stream correctly enabled for the current user and it returns a status code 204")
    void enableAudioStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true)));
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
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  null,
                  null,
                  null,
                  null)
              .audioStreamOn(false));
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"unmute\",\"room\":\"audioRoomId\",\"id\":\""
              + user1Id
              + "\"},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"success\"}}}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertTrue(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName(
        "If audio stream is already enabled for the current session, correctly it ignores and"
            + " returns a status code 204")
    void enableAudioStream_testOkAudioStreamAlreadyEnabledWithSessionEqualToCurrent()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertTrue(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName(
        "If the requested session isn't in the meeting participants, it returns a status code 404")
    void enableAudioStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void enableAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    void enableAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(true)),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable audio stream tests")
  class DisableAudioStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/audio", meetingId);
    }

    @Test
    @DisplayName("It disables the audio stream for the current user and returns a status code 204")
    void disableAudioStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
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
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  null,
                  null,
                  null,
                  null)
              .audioStreamOn(true));
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"mute\",\"room\":\"audioRoomId\",\"id\":\""
              + user1Id
              + "\"},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"success\"}}}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName(
        "If audio stream is already disabled for the current user, correctly it ignores and returns"
            + " a status code 204")
    void disableAudioStream_testOkAudioStreamAlreadyDisabledWithSessionEqualToCurrent()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("It disables the audio stream for another user and returns a status code 204")
    void disableAudioStream_testOkDisableWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(false)));
      VideoServerMeeting meeting =
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
                  meeting,
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .audioStreamOn(true));
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meeting,
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  null,
                  null,
                  null,
                  null)
              .audioStreamOn(true));
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"mute\",\"room\":\"audioRoomId\",\"id\":\""
              + user2Id
              + "\"},\"apisecret\":\"secret\"}",
          "{\"plugindata\":{\"data\":{\"audiobridge\":\"success\"}}}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  AudioStreamSettingsDto.create()
                      .enabled(false)
                      .userToModerate(user2Id.toString())),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user2Id.toString()).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName(
        "If audio stream is already disabled for another session, correctly it ignores and it"
            + " returns a status code 204")
    void disableAudioStream_testOkAudioStreamAlreadyDisabledWithAnotherSession() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  AudioStreamSettingsDto.create()
                      .enabled(false)
                      .userToModerate(user2Id.toString())),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user2Id.toString()).orElseThrow();
      assertFalse(participant.hasAudioStreamOn());
    }

    @Test
    @DisplayName("If current user isn't a room owner, it returns a status code 403")
    void disableAudioStream_testErrorCurrentUserNotRoomOwner() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id),
              RoomMemberField.create().id(user2Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  AudioStreamSettingsDto.create()
                      .enabled(false)
                      .userToModerate(user2Id.toString())),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "If the requested session isn't in the meeting participants, it returns a status code 404")
    void disableAudioStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  AudioStreamSettingsDto.create()
                      .enabled(false)
                      .userToModerate(user2Id.toString())),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void disableAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    void disableAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(AudioStreamSettingsDto.create().enabled(false)),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Enable screen stream tests")
  class EnableScreenStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/media", meetingId);
    }

    @Test
    @DisplayName(
        "Screen stream correctly enabled for the current session and it returns a status code 204")
    void enableScreenStream_testOkEnableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .screenStreamOn(false)));
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
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .screenStreamOn(false));
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/screenHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"publish\"},\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertTrue(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName(
        "If screen stream is already enabled for the current session, correctly it ignores and"
            + " returns a status code 204")
    void enableScreenStream_testOkScreenStreamAlreadyEnabledWithSessionEqualToCurrent()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .screenStreamOn(true)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertTrue(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName(
        "If the requested session isn't in the meeting participants, it returns a status code 404")
    void enableScreenStream_testErrorSessionNotFoundInMeetingParticipants() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .screenStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void enableScreenStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If sdp is not present then it returns a status code 400")
    void enableScreenStream_testErrorSdpNotPresent() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated then it returns a status code 401")
    void enableScreenStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("sdp")),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Disable screen stream tests")
  class DisableScreenStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/media", meetingId);
    }

    @Test
    @DisplayName(
        "It disables the screen stream for the current session and returns a status code 204")
    void disableScreenStream_testOkDisableWithSessionEqualToCurrent() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .screenStreamOn(true)));
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
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .screenStreamOn(true));
      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName(
        "If screen stream is already disabled for the current session, correctly it ignores and"
            + " returns a status code 204")
    void disableScreenStream_testOkScreenStreamAlreadyDisabledWithSessionEqualToCurrent()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .screenStreamOn(false)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
      Participant participant =
          meetingTestUtils.getParticipant(meetingId, user1Id.toString()).orElseThrow();
      assertFalse(participant.hasScreenStreamOn());
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void disableScreenStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    void disableScreenStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false)),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Update subscriptions media stream tests")
  class UpdateSubscriptionsMediaStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/media/subscribe", meetingId);
    }

    @Test
    @DisplayName(
        "It updates subscriptions for media stream joining as subscriber for the current session"
            + " and returns a status code 204")
    void updateSubscriptionsMediaStream_testOkJoinAsSubscriber() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(false)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
      VideoServerMeeting meeting =
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
                  meeting,
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .videoOutStreamOn(true));
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meeting,
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  "audioHandleId_" + user2Queue,
                  "videoOutHandleId_" + user2Queue,
                  "videoInHandleId_" + user2Queue,
                  "screenHandleId_" + user2Queue)
              .videoOutStreamOn(true));

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"join\",\"ptype\":\"subscriber\",\"room\":\"videoRoomId\",\"streams\":[{"
              + "\"feed\":\"82735f6d-4c6c-471e-99d9-4eef91b1ec45/video\"}]},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  SubscriptionUpdatesDto.create()
                      .subscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user2Id.toString())))),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "It updates subscriptions for media stream for a participant that already joined as"
            + " subscriber for the current session and returns a status code 204")
    void updateSubscriptionsMediaStream_testOkAlreadyJoinedAsSubscriber() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(false)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
      VideoServerMeeting meeting =
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
                  meeting,
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .videoOutStreamOn(true)
              .videoInStreamOn(true));
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meeting,
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  "audioHandleId_" + user2Queue,
                  "videoOutHandleId_" + user2Queue,
                  "videoInHandleId_" + user2Queue,
                  "screenHandleId_" + user2Queue)
              .videoOutStreamOn(true));

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"update\","
              + "\"subscribe\":[{\"feed\":\"82735f6d-4c6c-471e-99d9-4eef91b1ec45/video\"}]},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  SubscriptionUpdatesDto.create()
                      .subscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user2Id.toString())))),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "It updates subscriptions for media stream for the current session and returns a status"
            + " code 204")
    void updateSubscriptionsMediaStream_testOkBothSubscribeAndUnsubscribe() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(false),
              RoomMemberField.create().id(user3Id).owner(false)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
      VideoServerMeeting meeting =
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
                  meeting,
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .videoOutStreamOn(true)
              .videoInStreamOn(true));
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meeting,
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  "audioHandleId_" + user2Queue,
                  "videoOutHandleId_" + user2Queue,
                  "videoInHandleId_" + user2Queue,
                  "screenHandleId_" + user2Queue)
              .videoOutStreamOn(true));
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meeting,
                  user3Id.toString(),
                  user3Queue,
                  "connection_" + user3Queue,
                  "audioHandleId_" + user3Queue,
                  "videoOutHandleId_" + user3Queue,
                  "videoInHandleId_" + user3Queue,
                  "screenHandleId_" + user3Queue)
              .videoOutStreamOn(true));

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"update\",\"subscribe\":[{\"feed\":\"82735f6d-4c6c-471e-99d9-4eef91b1ec45/video\"}],"
              + "\"unsubscribe\":[{\"feed\":\"ea7b9b61-bef5-4cf4-80cb-19612c42593a/video\"}]},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  SubscriptionUpdatesDto.create()
                      .subscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user2Id.toString())))
                      .unsubscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user3Id.toString())))),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "It updates subscriptions for media stream unsubscribing for the current session and"
            + " returns a status code 204")
    void updateSubscriptionsMediaStream_testOkUnsubscribe() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id).owner(false)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
      VideoServerMeeting meeting =
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
                  meeting,
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .videoOutStreamOn(true)
              .videoInStreamOn(true));
      meetingTestUtils.updateVideoServerSession(
          meetingTestUtils
              .insertVideoServerSession(
                  meeting,
                  user2Id.toString(),
                  user2Queue,
                  "connection_" + user2Queue,
                  "audioHandleId_" + user2Queue,
                  "videoOutHandleId_" + user2Queue,
                  "videoInHandleId_" + user2Queue,
                  "screenHandleId_" + user2Queue)
              .videoOutStreamOn(true));

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"update\","
              + "\"unsubscribe\":[{\"feed\":\"82735f6d-4c6c-471e-99d9-4eef91b1ec45/video\"}]},\"apisecret\":\"secret\"}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(
                  SubscriptionUpdatesDto.create()
                      .unsubscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user2Id.toString())))),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If both subscribe and unsubscribe list are empty, it returns a status code 400")
    void updateSubscriptionsMediaStream_testErrorSubscribeListAndUnsubscribeListAreEmpty()
        throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .screenStreamOn(true)));

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(SubscriptionUpdatesDto.create()),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void updateSubscriptionsMediaStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  SubscriptionUpdatesDto.create()
                      .subscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user2Id.toString())))
                      .unsubscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user3Id.toString())))),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    void updateSubscriptionsMediaStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(
                  SubscriptionUpdatesDto.create()
                      .subscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user2Id.toString())))
                      .unsubscribe(
                          List.of(
                              MediaStreamDto.create()
                                  .type(MediaStreamDto.TypeEnum.VIDEO)
                                  .userId(user3Id.toString())))),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Answer rtc media stream tests")
  class AnswerRtcMediaStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/media/answer", meetingId);
    }

    @Test
    @DisplayName(
        "It answers with rtc for media stream with set handle id for the current session and"
            + " returns a status code 204")
    void answerRtcMediaStream_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true)));
      VideoServerMeeting meeting =
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
                  meeting,
                  user1Id.toString(),
                  user1Queue,
                  "connection_" + user1Queue,
                  "audioHandleId_" + user1Queue,
                  "videoOutHandleId_" + user1Queue,
                  "videoInHandleId_" + user1Queue,
                  "screenHandleId_" + user1Queue)
              .videoOutStreamOn(true));

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/videoInHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{\"request\":\"start\"},"
              + "\"apisecret\":\"secret\",\"jsep\":{\"type\":\"ANSWER\",\"sdp\":\"sdp\"}}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(SessionDescriptionProtocolDto.create().sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void answerRtcMediaStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(SessionDescriptionProtocolDto.create().sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    void answerRtcMediaStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(SessionDescriptionProtocolDto.create().sdp("sdp")),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Offer rtc audio stream tests")
  class OfferRtcAudioStreamTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/audio/offer", meetingId);
    }

    @Test
    @DisplayName(
        "It offers with rtc for audio stream with set handle id for the current session and returns"
            + " a status code 204")
    void offerRtcAudioStream_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("name")
              .description("description"),
          List.of(RoomMemberField.create().id(user1Id).owner(true)));
      UUID meetingId =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id, List.of(ParticipantBuilder.create(user1Id, user1Queue)));
      VideoServerMeeting meeting =
          meetingTestUtils.insertVideoServerMeeting(
              meetingId.toString(),
              "connectionId",
              "audioHandleId",
              "videoHandleId",
              "audioRoomId",
              "videoRoomId");
      meetingTestUtils.insertVideoServerSession(
          meeting,
          user1Id.toString(),
          user1Queue,
          "connection_" + user1Queue,
          "audioHandleId_" + user1Queue,
          "videoOutHandleId_" + user1Queue,
          "videoInHandleId_" + user1Queue,
          "screenHandleId_" + user1Queue);

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connection_" + user1Queue + "/audioHandleId_" + user1Queue,
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"join\",\"room\":\"audioRoomId\",\"id\":\"332a9527-3388-4207-be77-6d7e2978a723\",\"muted\":true,"
              + "\"filename\":\"${json-unit.ignore-element}\"},"
              + "\"apisecret\":\"secret\",\"jsep\":{\"type\":\"OFFER\",\"sdp\":\"sdp\"}}",
          "{\"janus\":\"ack\"}",
          true);

      MockHttpResponse response =
          dispatcher.put(
              url(meetingId),
              objectMapper.writeValueAsString(SessionDescriptionProtocolDto.create().sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the requested meeting doesn't exist, it returns a status code 404")
    void offerRtcAudioStream_testErrorMeetingNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(SessionDescriptionProtocolDto.create().sdp("sdp")),
              Map.of("queue-id", user1Queue),
              user1Token);

      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user isn’t authenticated, it returns a status code 401")
    void offerRtcAudioStream_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.put(
              url(UUID.randomUUID()),
              objectMapper.writeValueAsString(SessionDescriptionProtocolDto.create().sdp("sdp")),
              Map.of(),
              null);

      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Start meeting recording tests")
  class StartRecordingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/startRecording", meetingId);
    }

    @Test
    @DisplayName("Start a recording on a meeting")
    void startMeetingRecording_testOk() throws Exception {
      clock.fixTimeAt(Instant.parse("2024-03-18T13:23:17Z"));
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of());
      meetingTestUtils.insertVideoServerMeeting(
          meeting1Id.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"edit\",\"room\":\"videoRoomId\","
              + "\"new_rec_dir\":\"${json-unit.ignore-element}\"},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_mjrs\",\"room\":\"audioRoomId\","
              + "\"mjrs\":true,\"mjrs_dir\":\"${json-unit.ignore-element}\"},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_recording\",\"room\":\"videoRoomId\","
              + "\"record\":true},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);

      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Start a recording on a meeting which is already being recorded")
    void startMeetingRecording_testOkAlreadyStarted() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .token(user1Token)
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))));

      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Start a recording on a meeting which is not active")
    void startMeetingRecording_testErrorMeetingNotActive() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              false,
              List.of());

      MockHttpResponse response = dispatcher.post(url(meeting1Id), user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Start a recording on a meeting with a user that is not a moderator")
    void startMeetingRecording_testErrorUserIsNotAModerator() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of());

      MockHttpResponse response = dispatcher.post(url(meeting1Id), user2Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Stop meeting recording tests")
  class StopRecordingTests {

    private String url(UUID meetingId) {
      return String.format("/meetings/%s/stopRecording", meetingId);
    }

    @Test
    @DisplayName("Stop a recording on a meeting")
    void stopMeetingRecording_testOk() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .token(user1Token)
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))));
      meetingTestUtils.insertVideoServerMeeting(
          meeting1Id.toString(),
          "connectionId",
          "audioHandleId",
          "videoHandleId",
          "audioRoomId",
          "videoRoomId");

      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/audioHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_mjrs\",\"room\":\"audioRoomId\","
              + "\"mjrs\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoServerMockServer.mockRequestedResponse(
          "POST",
          "/janus/connectionId" + "/videoHandleId",
          "{\"janus\":\"message\",\"transaction\":\"${json-unit.ignore-element}\",\"body\":{"
              + "\"request\":\"enable_recording\",\"room\":\"videoRoomId\","
              + "\"record\":false},"
              + "\"apisecret\":\"secret\"}",
          "{\"janus\":\"success\"}",
          true);
      videoRecorderMockServer.mockRequestedResponse(
          "POST",
          "/PostProcessor/meeting_" + meeting1Id,
          objectMapper.writeValueAsString(
              VideoRecorderRequest.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName("Test Meeting for " + room1Id.toString())
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .authToken("ZM_AUTH_TOKEN=" + user1Token)
                  .folderId("rec-folder-id")
                  .recordingName("rec-name")),
          true);

      MockHttpResponse response =
          dispatcher.post(
              url(meeting1Id),
              objectMapper.writeValueAsString(
                  RecordingFieldsDto.create().name("rec-name").folderId("rec-folder-id")),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Stop a recording on a meeting which is already stopped")
    void stopMeetingRecording_testOkAlreadyStopped() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of());

      MockHttpResponse response =
          dispatcher.post(
              url(meeting1Id),
              objectMapper.writeValueAsString(
                  RecordingFieldsDto.create().name("rec-name").folderId("rec-folder-id")),
              user1Token);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Stop a recording on a meeting which is not active")
    void stopMeetingRecording_testErrorMeetingNotActive() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              false,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .token(user1Token)
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))));

      MockHttpResponse response =
          dispatcher.post(
              url(meeting1Id),
              objectMapper.writeValueAsString(
                  RecordingFieldsDto.create().name("rec-name").folderId("rec-folder-id")),
              user1Token);

      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Stop a recording on a meeting with a user that is not a moderator")
    void stopMeetingRecording_testErrorUserIsNotAModerator() throws Exception {
      integrationTestUtils.generateAndSaveRoom(
          Room.create()
              .id(room1Id.toString())
              .type(RoomTypeDto.GROUP)
              .name("room1")
              .description("Room one"),
          List.of(
              RoomMemberField.create().id(user1Id).owner(true),
              RoomMemberField.create().id(user2Id),
              RoomMemberField.create().id(user3Id)));
      UUID meeting1Id =
          meetingTestUtils.generateAndSaveMeeting(
              room1Id,
              MeetingType.PERMANENT,
              List.of(
                  ParticipantBuilder.create(user1Id, user1Queue)
                      .audioStreamOn(true)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user2Id, user2Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(true),
                  ParticipantBuilder.create(user3Id, user3Queue)
                      .audioStreamOn(false)
                      .videoStreamOn(false)),
              true,
              List.of(
                  Recording.create()
                      .id(UUID.randomUUID().toString())
                      .status(RecordingStatus.STARTED)
                      .starterId(user1Id.toString())
                      .token(user1Token)
                      .startedAt(OffsetDateTime.parse("2024-03-18T13:23:17Z"))));

      MockHttpResponse response =
          dispatcher.post(
              url(meeting1Id),
              objectMapper.writeValueAsString(
                  RecordingFieldsDto.create().name("rec-name").folderId("rec-folder-id")),
              user2Token);

      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }
}
