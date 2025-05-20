// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.WaitingParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.api.MeetingsApiService;
import com.zextras.carbonio.meeting.model.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MeetingsApiServiceImpl implements MeetingsApiService {

  private final MeetingService meetingService;
  private final RoomService roomService;
  private final ParticipantService participantService;
  private final WaitingParticipantService waitingParticipantService;

  @Inject
  public MeetingsApiServiceImpl(
      MeetingService meetingService,
      RoomService roomService,
      ParticipantService participantService,
      WaitingParticipantService waitingParticipantService) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.participantService = participantService;
    this.waitingParticipantService = waitingParticipantService;
  }

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  /**
   * Gets meetings list for authenticated user.
   *
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response
   * {@link Response) with status 200 and the meetings list {@link com.zextras.carbonio.meeting.model.MeetingDto} of
   * authenticated user in the body
   */
  @Override
  public Response listMeeting(SecurityContext securityContext) {
    return Response.ok()
        .entity(
            meetingService.getMeetings(
                Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
                    .orElseThrow(UnauthorizedException::new)))
        .build();
  }

  /**
   * Gets the requested meeting.
   *
   * @param meetingId       meeting identifier {@link UUID}}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response
   * {@link Response) with status 200 and the requested meeting {@link com.zextras.carbonio.meeting.model.MeetingDto} in
   * the body
   */
  @Override
  public Response getMeeting(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.ok().entity(meetingService.getMeetingById(meetingId, currentUser)).build();
  }

  /**
   * @param newMeetingDataDto data form creating a new meeting
   * @param securityContext   security context created by the authentication filter {@link SecurityContext}
   * @return a response
   * {@link Response) with status 200 and the requested meeting {@link com.zextras.carbonio.meeting.model.MeetingDto} in
   * the body
   */
  @Override
  public Response createMeeting(
      NewMeetingDataDto newMeetingDataDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    if (newMeetingDataDto.getRoomId() == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      return Response.ok(
              meetingService.createMeeting(
                  currentUser,
                  newMeetingDataDto.getName(),
                  newMeetingDataDto.getMeetingType(),
                  newMeetingDataDto.getRoomId(),
                  newMeetingDataDto.getExpiration()))
          .build();
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
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    meetingService.deleteMeetingById(meetingId, currentUser);
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
  public Response joinMeeting(
      UUID meetingId, JoinSettingsDto joinSettingsDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.status(Status.OK)
        .entity(
            new JoinMeetingResultDto()
                .status(
                    JoinStatusDto.fromString(
                        participantService
                            .insertMeetingParticipant(meetingId, joinSettingsDto, currentUser)
                            .toString())))
        .build();
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
    UserPrincipal currentUser = getCurrentUser(securityContext);
    participantService.removeMeetingParticipant(meetingId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Starts the meeting on the videoserver
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response
   * {@link Response) with status 200 and the updated meeting {@link com.zextras.carbonio.meeting.model.MeetingDto} in
   * the body
   */
  @Override
  public Response startMeeting(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    return Response.status(Status.OK)
        .entity(meetingService.startMeeting(currentUser, meetingId))
        .build();
  }

  /**
   * Stops the meeting on the videoserver
   *
   * @param meetingId       meeting identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response
   * {@link Response) with status 200 and the updated meeting {@link com.zextras.carbonio.meeting.model.MeetingDto} in
   * the body
   */
  @Override
  public Response stopMeeting(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    return Response.status(Status.OK)
        .entity(meetingService.stopMeeting(currentUser, meetingId))
        .build();
  }

  /**
   * Updates the media stream status in the meeting for the current session and starts WebRTC negotiation with
   * VideoServer for the PeerConnection setup related to screen stream when it has to be enabled.
   *
   * @param meetingId              meeting identifier {@link UUID}   *
   * @param mediaStreamSettingsDto user settings request to update the media stream status
   * @param securityContext        security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response updateMediaStream(
      UUID meetingId,
      MediaStreamSettingsDto mediaStreamSettingsDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (mediaStreamSettingsDto.isEnabled() && mediaStreamSettingsDto.getSdp() == null) {
      throw new BadRequestException(
          String.format(
              "User '%s' cannot enable the media stream without sending an rtc offer",
              currentUser.getId()));
    }
    participantService.updateMediaStream(meetingId, mediaStreamSettingsDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Updates the audio stream status in the meeting for the current session
   *
   * @param meetingId              meeting identifier {@link UUID}
   * @param audioStreamSettingsDto user settings request to update the audio stream status
   * @param securityContext        security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response updateAudioStream(
      UUID meetingId,
      AudioStreamSettingsDto audioStreamSettingsDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    participantService.updateAudioStream(meetingId, audioStreamSettingsDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Update subscriptions of the current session to the desired media streams
   *
   * @param meetingId              meeting identifier {@link UUID}
   * @param subscriptionUpdatesDto contains all media streams which user wants to update subscriptions for
   * @param securityContext        security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response updateSubscriptionsMediaStream(
      UUID meetingId,
      SubscriptionUpdatesDto subscriptionUpdatesDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (subscriptionUpdatesDto.getSubscribe().isEmpty()
        && subscriptionUpdatesDto.getUnsubscribe().isEmpty()) {
      throw new BadRequestException("Subscription list and Unsubscription list must not be empty");
    }
    participantService.updateSubscriptionsMediaStream(
        meetingId, subscriptionUpdatesDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Completes WebRTC negotiation with VideoServer for the PeerConnection setup related to media stream.
   *
   * @param meetingId                     meeting identifier {@link UUID}
   * @param sessionDescriptionProtocolDto the answer rtc session description
   * @param securityContext               security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response answerRtcMediaStream(
      UUID meetingId,
      SessionDescriptionProtocolDto sessionDescriptionProtocolDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    participantService.answerRtcMediaStream(
        meetingId, sessionDescriptionProtocolDto.getSdp(), currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Starts WebRTC negotiation with VideoServer for the PeerConnection setup related to audio stream.
   *
   * @param meetingId                     meeting identifier {@link UUID}
   * @param sessionDescriptionProtocolDto the offer rtc session description
   * @param securityContext               security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 204
   */
  @Override
  public Response offerRtcAudioStream(
      UUID meetingId,
      SessionDescriptionProtocolDto sessionDescriptionProtocolDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    participantService.offerRtcAudioStream(
        meetingId, sessionDescriptionProtocolDto.getSdp(), currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response getQueue(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    return meetingService
        .getMeetingEntity(meetingId)
        .map(
            meeting -> {
              roomService.getRoomAndValidateUser(
                  UUID.fromString(meeting.getRoomId()), currentUser, true);
              return meeting;
            })
        .map(
            meeting ->
                Response.status(Status.OK)
                    .entity(
                        new QueuedUsersDto().users(waitingParticipantService.getQueue(meetingId)))
                    .build())
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  public Response updateQueuedUser(
      UUID meetingId,
      UUID userId,
      QueuedUserUpdateDto queuedUserUpdateDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    waitingParticipantService.updateQueue(
        meetingId, userId, queuedUserUpdateDto.getStatus(), currentUser);
    return Response.noContent().build();
  }

  @Override
  public Response startRecording(UUID meetingId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    meetingService.startMeetingRecording(meetingId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response stopRecording(
      UUID meetingId, RecordingFieldsDto recordingFieldsDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    meetingService.stopMeetingRecording(
        meetingId, recordingFieldsDto.getName(), recordingFieldsDto.getFolderId(), currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response updateHandStatus(
      UUID meetingId, HandStatusDto handStatusDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    participantService.updateHandStatus(meetingId, handStatusDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }
}
