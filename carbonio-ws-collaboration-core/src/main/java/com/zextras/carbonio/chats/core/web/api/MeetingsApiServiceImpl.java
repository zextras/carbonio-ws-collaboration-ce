// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.api.MeetingsApiService;
import com.zextras.carbonio.meeting.api.NotFoundException;
import com.zextras.carbonio.meeting.model.AudioStreamSettingsDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.NewMeetingDataDto;

import com.zextras.carbonio.meeting.model.ScreenStreamSettingsDto;
import com.zextras.carbonio.meeting.model.VideoStreamSettingsDto;
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
   * @return a response
   * {@link Response) with status 200 and the meetings list {@link MeetingDto} of authenticated user in the body
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



  @Override
  public Response createMeeting(NewMeetingDataDto newMeetingDataDto, SecurityContext securityContext){
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if(newMeetingDataDto.getName() == null && newMeetingDataDto.getUsers().isEmpty()){
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      return Response.ok(
        meetingService.createMeeting(
          currentUser,
          newMeetingDataDto.getName(),
          newMeetingDataDto.getMeetingType(),
          newMeetingDataDto.getRoomId(),
          newMeetingDataDto.getUsers(),
          newMeetingDataDto.getExpiration()
        )
      ).build();
    }
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
   * @param joinSettingsDto user requested access settings for meeting
   *                        {@link com.zextras.carbonio.meeting.model.JoinSettingsDto}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response joinMeeting(UUID meetingId, JoinSettingsDto joinSettingsDto,
    SecurityContext securityContext) {
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
   * Updates the video stream status in the meeting for the current session
   *
   * @param meetingId              meeting identifier {@link UUID}
   * @param sessionId              identifier of the user session whose video stream status has to updated
   * @param videoStreamSettingsDto user settings request to update the video stream status
   * @param securityContext        security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response updateVideoStream(UUID meetingId, String sessionId, VideoStreamSettingsDto videoStreamSettingsDto,
    SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.updateVideoStream(meetingId, sessionId, videoStreamSettingsDto.isEnabled(), currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response startMeeting(UUID meetingId, SecurityContext securityContext){
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(meetingService.updateMeeting(currentUser, meetingId, true))
      .build();
  }

  @Override
  public Response stopMeeting(UUID meetingId, SecurityContext securityContext) throws NotFoundException {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(meetingService.updateMeeting(currentUser, meetingId, false))
      .build();
  }

  /**
   * Updates the audio stream status in the meeting for the current session
   *
   * @param meetingId              meeting identifier {@link UUID}
   * @param sessionId              identifier of the user session whose audio stream status has to updated
   * @param audioStreamSettingsDto user settings request to update the audio stream status
   * @param securityContext        security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response updateAudioStream(UUID meetingId, String sessionId, AudioStreamSettingsDto audioStreamSettingsDto,
    SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.updateAudioStream(meetingId, sessionId, audioStreamSettingsDto.isEnabled(), currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Updates the screen stream status in the meeting for the current session
   *
   * @param meetingId               meeting identifier {@link UUID}
   * @param sessionId               identifier of the user session whose screen stream status has to updated
   * @param screenStreamSettingsDto user settings request to update the screen stream status
   * @param securityContext         security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response updateScreenStream(UUID meetingId, String sessionId, ScreenStreamSettingsDto screenStreamSettingsDto,
    SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (currentUser.getSessionId() == null || currentUser.getSessionId().isEmpty()) {
      throw new BadRequestException("Session identifier is mandatory");
    }
    participantService.updateScreenStream(meetingId, sessionId, screenStreamSettingsDto.isEnabled(), currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }
}
