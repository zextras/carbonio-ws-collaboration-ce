// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingService {

  MeetingDto createMeeting(
      UserPrincipal user,
      String name,
      MeetingTypeDto meetingType,
      UUID roomId,
      OffsetDateTime expiration);

  MeetingDto updateMeeting(UserPrincipal user, UUID meetingId, Boolean started);

  List<MeetingDto> getMeetings(UserPrincipal currentUser);

  MeetingDto getMeetingById(UUID meetingId, UserPrincipal currentUser);

  Optional<Meeting> getMeetingEntity(UUID meetingId);

  Optional<Meeting> getMeetingEntityByRoomId(UUID roomId);

  MeetingDto getMeetingByRoomId(UUID roomId, UserPrincipal currentUser);

  void deleteMeetingById(UUID meetingId, UserPrincipal currentUser);

  void deleteMeeting(Meeting meeting, Room room, UUID userId);
}
