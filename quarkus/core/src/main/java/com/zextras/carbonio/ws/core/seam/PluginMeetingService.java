// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStarted;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStopping;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * The CE meeting service with its ADV extension hooks turned into plugin CDI events, as ADV
 * subclassed it under Guice:
 *
 * <ul>
 *   <li>{@code stopActiveRecording} → synchronous {@link MeetingStopping}, fired by stop, last
 *       leave (via stop) and delete before the videoserver teardown, only when the meeting is
 *       active (an inactive meeting has nothing to stop: no event);
 *   <li>{@code afterMeetingStarted} → {@link MeetingStarted}.
 * </ul>
 */
@Singleton // the CE superclass has no no-args constructor, so no client proxy
@Alternative
@Priority(1)
public class PluginMeetingService extends MeetingServiceImpl {

  private final Event<MeetingStopping> meetingStopping;
  private final Event<MeetingStarted> meetingStarted;

  @Inject
  public PluginMeetingService(
      MeetingRepository meetingRepository,
      MeetingMapper meetingMapper,
      RoomService roomService,
      MembersService membersService,
      ParticipantService participantService,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      Clock clock,
      MessageDispatcher messageDispatcher,
      Event<MeetingStopping> meetingStopping,
      Event<MeetingStarted> meetingStarted) {
    super(
        meetingRepository,
        meetingMapper,
        roomService,
        membersService,
        participantService,
        videoServerService,
        eventDispatcher,
        clock,
        messageDispatcher);
    this.meetingStopping = meetingStopping;
    this.meetingStarted = meetingStarted;
  }

  @Override
  protected void stopActiveRecording(String userId, Meeting meeting) {
    if (!Boolean.TRUE.equals(meeting.getActive())) {
      return;
    }
    meetingStopping.fire(
        new MeetingStopping(
            UUID.fromString(meeting.getId()), userId == null ? null : UUID.fromString(userId)));
  }

  @Override
  protected void afterMeetingStarted(UserPrincipal user, RoomDto room, List<String> receivers) {
    if (room.getMeetingId() != null) {
      meetingStarted.fire(new MeetingStarted(room.getMeetingId(), room.getId(), user.getUUID()));
    }
  }
}
