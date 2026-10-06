// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import com.zextras.carbonio.ws.core.meeting.dto.CreateMeetingRequest;
import com.zextras.carbonio.ws.core.meeting.dto.JoinResponse;
import com.zextras.carbonio.ws.core.meeting.dto.MeetingDto;
import com.zextras.carbonio.ws.plugin.api.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/meetings")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeetingResource {

  @Inject MeetingService meetings;
  @Inject CurrentUser currentUser;

  @POST
  public MeetingDto create(CreateMeetingRequest request) {
    return meetings.create(currentUser.get(), request);
  }

  @GET
  @Path("/{meetingId}")
  public MeetingDto get(@PathParam("meetingId") UUID meetingId) {
    return meetings.get(currentUser.get(), meetingId);
  }

  @POST
  @Path("/{meetingId}/start")
  public MeetingDto start(@PathParam("meetingId") UUID meetingId) {
    return meetings.start(currentUser.get(), meetingId);
  }

  @POST
  @Path("/{meetingId}/stop")
  public MeetingDto stop(@PathParam("meetingId") UUID meetingId) {
    return meetings.stop(currentUser.get(), meetingId);
  }

  @POST
  @Path("/{meetingId}/join")
  public JoinResponse join(@PathParam("meetingId") UUID meetingId) {
    return new JoinResponse(meetings.join(currentUser.get(), meetingId));
  }

  @POST
  @Path("/{meetingId}/leave")
  public void leave(@PathParam("meetingId") UUID meetingId) {
    meetings.leave(currentUser.get(), meetingId);
  }

  @DELETE
  @Path("/{meetingId}")
  public void delete(@PathParam("meetingId") UUID meetingId) {
    meetings.delete(currentUser.get(), meetingId);
  }
}
