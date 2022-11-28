package com.zextras.carbonio.chats.meeting.web.api;

import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.api.RoomsApiService;
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
public class RoomsApiServiceImpl implements RoomsApiService {

  private final MeetingService meetingService;

  @Inject
  public RoomsApiServiceImpl(MeetingService meetingService) {
    this.meetingService = meetingService;
  }

  /**
   * Created a meeting associated to the room
   *
   * @param roomId          room identifier {@link UUID}
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 201 and the created meeting {@link MeetingDto } in the body
   */
  @Override
  public Response createMeetingByRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(meetingService.createMeetingByRoomId(roomId, currentUser))
      .build();
  }

  /**
   * Gets the meeting of requested room
   *
   * @param roomId          room identifier
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 200 and the requested meeting {@link MeetingDto } in the body
   */
  @Override
  public Response getMeetingByRoomId(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.ok().entity(meetingService.getMeetingByRoomId(roomId, currentUser)).build();
  }

}
