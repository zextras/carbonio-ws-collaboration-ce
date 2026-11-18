// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.api.MeetingsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.MessageBrokerVideoserverHealthMonitor;
import com.zextras.carbonio.chats.core.infrastructure.websocket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.infrastructure.websocket.SessionPingManager;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.JoinSettingsDto;
import com.zextras.carbonio.chats.model.MediaQualityRequestDto;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import com.zextras.carbonio.chats.model.NewMeetingDataDto;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class MeetingsApiServiceImplTest {

  private final MeetingsApiService meetingsApiService;
  private final MeetingService meetingService;
  private final ParticipantService participantService;
  private final SecurityContext securityContext;
  private final MessageBrokerVideoserverHealthMonitor healthMonitor;
  private UUID user1Id;
  private UUID roomId;
  private UUID meetingId;
  private UserPrincipal user1;

  public MeetingsApiServiceImplTest() {
    this.securityContext = mock(SecurityContext.class);
    this.meetingService = mock(MeetingService.class);
    this.participantService = mock(ParticipantService.class);
    CacheVideoServerSession cacheVideoServerSession = mock(CacheVideoServerSession.class);
    this.healthMonitor = mock(MessageBrokerVideoserverHealthMonitor.class);
    this.meetingsApiService =
        new MeetingsApiServiceImpl(
            meetingService,
            new MeetingMapperImpl(new ParticipantMapperImpl()),
            participantService,
            cacheVideoServerSession,
            healthMonitor);
  }

  @BeforeEach
  void init() {
    user1Id = UUID.randomUUID();

    user1 =
        UserPrincipal.create(user1Id)
            .userType(UserType.INTERNAL)
            .carbonioAttributes(
                Map.of(
                    CarbonioAttribute.WSC_VIDEO_CALL_ENABLED.getValue(),
                    "TRUE",
                    CarbonioAttribute.WSC_RECORDING_ENABLED.getValue(),
                    "TRUE"));

    roomId = UUID.randomUUID();
    meetingId = UUID.randomUUID();
  }

  @AfterEach
  void afterEach() {
    reset(meetingService, participantService, securityContext, healthMonitor);
  }

  @Test
  void cannotCreateWithoutRoomId() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    Response response =
        meetingsApiService.createMeeting(NewMeetingDataDto.create(), securityContext);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void authenticatedUserCreateMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    Response response =
        meetingsApiService.createMeeting(
            NewMeetingDataDto.create().roomId(roomId), securityContext);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void authenticatedUserDeleteMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    Response response = meetingsApiService.deleteMeeting(meetingId, securityContext);

    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  void authenticatedUserStartMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    Response response = meetingsApiService.startMeeting(meetingId, securityContext);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void authenticatedUserStopMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    Response response = meetingsApiService.stopMeeting(meetingId, securityContext);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void authenticatedUserJoinMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(participantService.insertMeetingParticipant(meetingId, JoinSettingsDto.create(), user1))
        .thenReturn(JoinStatus.ACCEPTED);

    Response response =
        meetingsApiService.joinMeeting(meetingId, JoinSettingsDto.create(), securityContext);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void joinMeeting_userWebSocketOnAnotherInstance_isAccepted() throws Exception {
    MessageBrokerVideoserverHealthMonitor realMonitor =
        new MessageBrokerVideoserverHealthMonitor(
            new EventWebSocketSessions(new SessionPingManager()), new JacksonConfig().get());
    realMonitor.notifyVideoServerReady();
    MeetingsApiServiceImpl service =
        new MeetingsApiServiceImpl(
            meetingService,
            new MeetingMapperImpl(new ParticipantMapperImpl()),
            participantService,
            mock(CacheVideoServerSession.class),
            realMonitor);
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(participantService.insertMeetingParticipant(meetingId, JoinSettingsDto.create(), user1))
        .thenReturn(JoinStatus.ACCEPTED);

    Response response = service.joinMeeting(meetingId, JoinSettingsDto.create(), securityContext);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void authenticatedUserWithFeatureDisabledCannotStartMeeting() {
    user1.carbonioAttributes(Map.of(CarbonioAttribute.WSC_VIDEO_CALL_ENABLED.getValue(), "FALSE"));
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        ForbiddenException.class,
        () -> meetingsApiService.startMeeting(meetingId, securityContext));
  }

  @Test
  void authenticatedUserWithFeatureDisabledCannotJoinMeeting() {
    user1.carbonioAttributes(Map.of(CarbonioAttribute.WSC_VIDEO_CALL_ENABLED.getValue(), "FALSE"));
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        ForbiddenException.class,
        () -> meetingsApiService.joinMeeting(meetingId, JoinSettingsDto.create(), securityContext));
  }

  @Test
  void messageBrokerDownBlocksJoinMeeting() {
    doThrow(new EventDispatcherException("Message broker is down"))
        .when(healthMonitor)
        .checkStatus();
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        EventDispatcherException.class,
        () -> meetingsApiService.joinMeeting(meetingId, JoinSettingsDto.create(), securityContext));
  }

  @Test
  void messageBrokerDownBlocksStartMeeting() {
    doThrow(new EventDispatcherException("Message broker is down"))
        .when(healthMonitor)
        .checkStatus();
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        EventDispatcherException.class,
        () -> meetingsApiService.startMeeting(meetingId, securityContext));
  }

  @Test
  void messageBrokerDownBlocksUpdateMediaStream() {
    doThrow(new EventDispatcherException("Message broker is down"))
        .when(healthMonitor)
        .checkStatus();
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        EventDispatcherException.class,
        () ->
            meetingsApiService.updateMediaStream(
                meetingId,
                com.zextras.carbonio.chats.model.MediaStreamSettingsDto.create()
                    .enabled(false)
                    .type(com.zextras.carbonio.chats.model.MediaStreamSettingsDto.TypeEnum.VIDEO),
                securityContext));
  }

  @Test
  void messageBrokerDownBlocksUpdateAudioStream() {
    doThrow(new EventDispatcherException("Message broker is down"))
        .when(healthMonitor)
        .checkStatus();
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        EventDispatcherException.class,
        () ->
            meetingsApiService.updateAudioStream(
                meetingId,
                com.zextras.carbonio.chats.model.AudioStreamSettingsDto.create().enabled(false),
                securityContext));
  }

  @Test
  void messageBrokerDownBlocksUpdateHandStatus() {
    doThrow(new EventDispatcherException("Message broker is down"))
        .when(healthMonitor)
        .checkStatus();
    when(securityContext.getUserPrincipal()).thenReturn(user1);

    assertThrows(
        EventDispatcherException.class,
        () ->
            meetingsApiService.updateHandStatus(
                meetingId,
                com.zextras.carbonio.chats.model.HandStatusDto.create().raised(false),
                securityContext));
  }

  @Test
  void updateSubscribedMediaQuality_returns204() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    MediaQualityRequestDto dto =
        MediaQualityRequestDto.create()
            .userId(UUID.randomUUID().toString())
            .type(MediaQualityRequestDto.TypeEnum.VIDEO)
            .mid("0")
            .substream(1);

    Response response =
        meetingsApiService.updateSubscribedMediaQuality(meetingId, dto, securityContext);

    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    verify(participantService, times(1)).updateSubscribedMediaQuality(meetingId, dto, user1);
  }

  private Meeting activeMeeting(UUID id) {
    return Meeting.create()
        .id(id.toString())
        .roomId(roomId.toString())
        .name("meeting")
        .meetingType(MeetingType.SCHEDULED)
        .active(true)
        .startedAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
        .participants(
            List.of(
                Participant.create()
                    .userId(user1Id.toString())
                    .queueId("queue")
                    .audioStreamOn(true)
                    .videoStreamOn(false)));
  }

  private void assertMeetingDto(UUID id, Object entity) {
    MeetingDto dto = (MeetingDto) entity;
    assertEquals(id, dto.getId());
    assertEquals(roomId, dto.getRoomId());
    assertEquals("meeting", dto.getName());
    assertEquals(MeetingTypeDto.SCHEDULED, dto.getMeetingType());
    assertTrue(dto.isActive());
    assertEquals(OffsetDateTime.parse("2022-01-01T13:00:00Z"), dto.getStartedAt());
    assertEquals(1, dto.getParticipants().size());
    assertEquals(user1Id, dto.getParticipants().get(0).getUserId());
    assertEquals("queue", dto.getParticipants().get(0).getQueueId());
    assertTrue(dto.getParticipants().get(0).isAudioStreamEnabled());
  }

  @Test
  void listMeeting_mapsEveryMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(meetingService.getMeetings(user1)).thenReturn(List.of(activeMeeting(meetingId)));

    Response response = meetingsApiService.listMeeting(securityContext);

    verify(meetingService, times(1)).getMeetings(user1);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    List<?> body = (List<?>) response.getEntity();
    assertEquals(1, body.size());
    assertMeetingDto(meetingId, body.get(0));
  }

  @Test
  void getMeeting_mapsTheMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(meetingService.getMeetingById(meetingId, user1)).thenReturn(activeMeeting(meetingId));

    Response response = meetingsApiService.getMeeting(meetingId, securityContext);

    verify(meetingService, times(1)).getMeetingById(meetingId, user1);
    assertMeetingDto(meetingId, response.getEntity());
  }

  @Test
  void createMeeting_mapsTheCreatedMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(meetingService.createMeeting(user1, "meeting", MeetingTypeDto.SCHEDULED, roomId, null))
        .thenReturn(activeMeeting(meetingId));

    Response response =
        meetingsApiService.createMeeting(
            NewMeetingDataDto.create()
                .name("meeting")
                .meetingType(MeetingTypeDto.SCHEDULED)
                .roomId(roomId),
            securityContext);

    verify(meetingService, times(1))
        .createMeeting(user1, "meeting", MeetingTypeDto.SCHEDULED, roomId, null);
    assertMeetingDto(meetingId, response.getEntity());
  }

  @Test
  void startMeeting_mapsTheStartedMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(meetingService.startMeeting(user1, meetingId)).thenReturn(activeMeeting(meetingId));

    Response response = meetingsApiService.startMeeting(meetingId, securityContext);

    verify(meetingService, times(1)).startMeeting(user1, meetingId);
    assertMeetingDto(meetingId, response.getEntity());
  }

  @Test
  void stopMeeting_mapsTheStoppedMeeting() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(meetingService.stopMeeting(user1, meetingId)).thenReturn(activeMeeting(meetingId));

    Response response = meetingsApiService.stopMeeting(meetingId, securityContext);

    verify(meetingService, times(1)).stopMeeting(user1, meetingId);
    assertMeetingDto(meetingId, response.getEntity());
  }
}
