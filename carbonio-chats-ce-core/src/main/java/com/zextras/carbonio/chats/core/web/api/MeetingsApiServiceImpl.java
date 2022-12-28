// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.api.MeetingsApiService;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class MeetingsApiServiceImpl implements MeetingsApiService {

  private final MeetingService     meetingService;
  private final ParticipantService participantService;

  @Inject
  public MeetingsApiServiceImpl(
    MeetingService meetingService, ParticipantService participantService
  ) {
    this.meetingService = meetingService;
    this.participantService = participantService;
  }

  /**
   * Gets meetings list for authenticated user.
   *
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 200 and the meetings list {@link MeetingDto } of authenticated user
   * in the body
   */
  @Override
  public Response listMeeting(SecurityContext securityContext) {
    return Response.ok().entity(meetingService.getMeetings(
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
          .orElseThrow(UnauthorizedException::new)))
      .build();
  }

  /**
   * Gets the requested meeting.
   *
   * @param meetingId       meeting identifier {@link UUID}}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 200 and the requested meeting {@link MeetingDto} in the body
   */
  @Override
  public Response getMeeting(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.ok().entity(meetingService.getMeetingById(meetingId, currentUser)).build();
  }

  /**
   * Deletes the requested meeting.
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response deleteMeeting(UUID meetingId, SecurityContext securityContext) {
    meetingService.deleteMeetingById(meetingId,
      Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Allows the authenticated user to join a meeting.
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param joinSettingsDto user requested access settings {@link JoinSettingsDto}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response joinMeeting(UUID meetingId, JoinSettingsDto joinSettingsDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.insertMeetingParticipant(meetingId, joinSettingsDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Removes the authenticated user from the meeting.
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response leaveMeeting(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.removeMeetingParticipant(meetingId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Opens the video stream in the meeting for the current session
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response openVideoStream(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.enableVideoStream(meetingId, currentUser.getSessionId(), true, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Closes the video stream in the meeting for the session
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param sessionId       identifier of the session to close
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response closeVideoStream(UUID meetingId, String sessionId, SecurityContext securityContext) {
    participantService.enableVideoStream(meetingId, sessionId, false,
      Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Opens the screen share stream in the meeting for the current session
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response openScreenShareStream(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.enableScreenShareStream(meetingId, currentUser.getSessionId(), true, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Closes the screen share stream in the meeting for the session
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param sessionId       identifier of the session to close
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response closeScreenShareStream(UUID meetingId, String sessionId, SecurityContext securityContext) {
    participantService.enableScreenShareStream(meetingId, sessionId, false,
      Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Opens the audio stream in the meeting for the current session
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response openAudioStream(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.enableAudioStream(meetingId, currentUser.getSessionId(), true, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Closes the audio stream in the meeting for the session
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param sessionId       identifier of the session to close
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response closeAudioStream(UUID meetingId, String sessionId, SecurityContext securityContext) {
    participantService.enableAudioStream(meetingId, sessionId, false,
      Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

}
