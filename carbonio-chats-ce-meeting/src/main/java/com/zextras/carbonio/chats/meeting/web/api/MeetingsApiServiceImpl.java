package com.zextras.carbonio.chats.meeting.web.api;

import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class MeetingsApiServiceImpl implements MeetingsApiService {

  private final MeetingService meetingService;

  @Inject
  public MeetingsApiServiceImpl(MeetingService meetingService) {
    this.meetingService = meetingService;
  }

  /**
   * Gets meetings list for authenticated user.
   *
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 200 and the meetings list {@link MeetingDto} of authenticated user
   * in the body
   */
  @Override
  public Response listMeeting(SecurityContext securityContext) {
    return Response.status(Status.NOT_IMPLEMENTED).build();
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
    return Response.status(Status.NOT_IMPLEMENTED).build();
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
    return Response.status(Status.NOT_IMPLEMENTED).build();
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
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }
}
