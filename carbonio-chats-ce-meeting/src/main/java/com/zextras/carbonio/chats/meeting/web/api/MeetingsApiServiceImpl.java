package com.zextras.carbonio.chats.meeting.web.api;

import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.api.NotFoundException;
import com.zextras.carbonio.chats.meeting.api.RoomsApiService;
import com.zextras.carbonio.chats.meeting.model.JoinSettingsDto;
import java.util.UUID;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class MeetingsApiServiceImpl implements MeetingsApiService, RoomsApiService {

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
    return Response.status(Status.NOT_IMPLEMENTED).build();
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
