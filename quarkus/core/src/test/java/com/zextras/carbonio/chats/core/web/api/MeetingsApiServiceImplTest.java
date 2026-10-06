// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.api.MeetingsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import com.zextras.carbonio.chats.model.JoinSettingsDto;
import com.zextras.carbonio.chats.model.NewMeetingDataDto;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
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
            meetingService, participantService, cacheVideoServerSession, healthMonitor);
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
}
