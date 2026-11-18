// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingService {

  Meeting createMeeting(
      UserPrincipal user,
      String name,
      MeetingTypeDto meetingType,
      UUID roomId,
      OffsetDateTime expiration);

  Meeting startMeeting(UserPrincipal user, UUID meetingId);

  Meeting stopMeeting(UserPrincipal user, UUID meetingId);

  List<Meeting> getMeetings(UserPrincipal currentUser);

  Meeting getMeetingById(UUID meetingId, UserPrincipal currentUser);

  Optional<Meeting> getMeetingEntity(UUID meetingId);

  Optional<Meeting> getMeetingEntityByRoomId(UUID roomId);

  Meeting getMeetingByRoomId(UUID roomId, UserPrincipal currentUser);

  void deleteMeetingById(UUID meetingId, UserPrincipal currentUser);

  void deleteMeeting(String userId, Meeting meeting, Room room);

  /**
   * Runs the I/O that must happen before a meeting row is deleted: the stop hooks of the edition
   * and the video-server stop. It touches no database row of the meeting.
   *
   * @param userId identifier of the user on whose behalf the meeting is released
   * @param meeting the meeting to release
   */
  void releaseMeetingResources(String userId, Meeting meeting);

  void updateMeeting(Meeting updatedMeeting);

  void declineMeeting(UUID meetingId, UserPrincipal currentUser);
}
