// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.meeting.api.PublicApiService;
import com.zextras.carbonio.meeting.model.*;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class PublicMeetingsApiServiceImpl implements PublicApiService {

  private final MeetingService meetingService;

  @Inject
  public PublicMeetingsApiServiceImpl(MeetingService meetingService) {
    this.meetingService = meetingService;
  }

  @Override
  public Response getMeetingPublic(UUID meetingId, SecurityContext securityContext) {
    return meetingService
        .getMeetingEntity(meetingId)
        .filter(meeting -> meeting.getMeetingType().equals(MeetingType.SCHEDULED))
        .map(meeting -> Response.ok(new PublicMeetingDto().name(meeting.getName())).build())
        .orElse(Response.status(Status.NOT_FOUND).build());
  }
}
