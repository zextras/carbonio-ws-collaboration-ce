// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.WaitingParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.api.MeetingsApiService;
import com.zextras.carbonio.meeting.model.NewMeetingDataDto;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class MeetingsApiServiceImplTest {

  private final MeetingsApiService meetingsApiService;
  private final MeetingService meetingService;
  private final RoomService roomService;
  private final ParticipantService participantService;
  private final WaitingParticipantService waitingParticipantService;
  private final SecurityContext securityContext;
  private UUID user1Id;
  private UUID guestId;
  private UUID roomId;
  private UUID meetingId;
  private UserPrincipal user1;
  private UserPrincipal guest;

  public MeetingsApiServiceImplTest() {
    this.securityContext = mock(SecurityContext.class);
    this.meetingService = mock(MeetingService.class);
    this.roomService = mock(RoomService.class);
    this.participantService = mock(ParticipantService.class);
    this.waitingParticipantService = mock(WaitingParticipantService.class);
    this.meetingsApiService =
        new MeetingsApiServiceImpl(
            meetingService, roomService, participantService, waitingParticipantService);
  }

  @BeforeEach
  void init() {
    guestId = UUID.randomUUID();
    user1Id = UUID.randomUUID();

    user1 = UserPrincipal.create(user1Id).userType(UserType.INTERNAL);
    guest = UserPrincipal.create(guestId).userType(UserType.GUEST);

    roomId = UUID.randomUUID();
    meetingId = UUID.randomUUID();
  }

  @AfterEach
  void afterEach() {
    reset(
        meetingService,
        roomService,
        participantService,
        waitingParticipantService,
        securityContext);
  }

  @Test
  void guestCannotDo() {
    when(securityContext.getUserPrincipal()).thenReturn(guest);

    var newMeetingData = NewMeetingDataDto.create().roomId(roomId);
    assertThrows(
        ForbiddenException.class,
        () ->
            meetingsApiService.createMeeting(newMeetingData, securityContext));

    assertThrows(
        ForbiddenException.class,
        () -> meetingsApiService.deleteMeeting(meetingId, securityContext));

    assertThrows(
        ForbiddenException.class,
        () -> meetingsApiService.startMeeting(meetingId, securityContext));

    assertThrows(
        ForbiddenException.class, () -> meetingsApiService.stopMeeting(meetingId, securityContext));

    assertThrows(
        ForbiddenException.class, () -> meetingsApiService.getQueue(meetingId, securityContext));

    assertThrows(
        ForbiddenException.class, () -> meetingsApiService.updateQueuedUser(meetingId, user1Id, null, securityContext));

    assertThrows(
        ForbiddenException.class, () -> meetingsApiService.startRecording(meetingId, securityContext));

    assertThrows(
        ForbiddenException.class, () -> meetingsApiService.stopRecording(meetingId, null, securityContext));
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
}
