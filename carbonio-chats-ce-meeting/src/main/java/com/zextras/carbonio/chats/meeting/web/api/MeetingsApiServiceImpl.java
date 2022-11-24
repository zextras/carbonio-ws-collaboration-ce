package com.zextras.carbonio.chats.meeting.web.api;

import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.api.NotFoundException;
import com.zextras.carbonio.chats.meeting.api.RoomsApiService;
import com.zextras.carbonio.chats.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class MeetingsApiServiceImpl implements MeetingsApiService, RoomsApiService {

  private final MeetingService meetingService;

  @Inject
  public MeetingsApiServiceImpl(MeetingService meetingService) {
    this.meetingService = meetingService;
  }

  @Override
  public Response listMeeting(SecurityContext securityContext) throws NotFoundException {
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }

  @Override
  public Response getMeeting(UUID meetingId, SecurityContext securityContext) throws NotFoundException {
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }

  @Override
  public Response getMeetingByRoom(UUID roomId, SecurityContext securityContext) throws NotFoundException {
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }

  @Override
  public Response createMeetingByRoom(UUID roomId, SecurityContext securityContext) throws NotFoundException {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(meetingService.createMeetingByRoom(roomId, currentUser))
      .build();
  }

  @Override
  public Response deleteMeeting(UUID meetingId, SecurityContext securityContext) throws NotFoundException {
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }

  @Override
  public Response joinMeeting(
    UUID meetingId, JoinSettingsDto joinSettingsDto, SecurityContext securityContext
  ) throws NotFoundException {
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }

  @Override
  public Response leaveMeeting(UUID meetingId, SecurityContext securityContext) throws NotFoundException {
    return Response.status(Status.NOT_IMPLEMENTED).build();
  }
}
