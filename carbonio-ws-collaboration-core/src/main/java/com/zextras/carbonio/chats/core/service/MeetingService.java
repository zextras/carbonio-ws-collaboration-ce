// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingService {

  /**
   * Creates a new meeting
   *
   * @param user who wants to create the meeting
   * @param name the name chosen for the meeting
   * @param meetingType the type chosen for the meeting
   * @param roomId room identifier {@link UUID}
   * @param expiration meeting expiration timestamp
   * @return {@link MeetingDto}
   */
  MeetingDto createMeeting(
      UserPrincipal user,
      String name,
      MeetingTypeDto meetingType,
      UUID roomId,
      OffsetDateTime expiration);

  /**
   * Updates a meeting
   *
   * @param user who wants to update the meeting status
   * @param meetingId meeting identifier {@link UUID}
   * @param started boolean used to update the meeting status
   * @return {@link MeetingDto}
   */
  MeetingDto updateMeeting(UserPrincipal user, UUID meetingId, Boolean started);

  /**
   * Gets the meetings list of all rooms where the authenticated user is a member
   *
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The {@link List} of {@link MeetingDto} of all rooms where the authenticated user is a
   *     member
   */
  List<MeetingDto> getMeetings(UserPrincipal currentUser);

  /**
   * Gets indicated meeting data
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The indicated meeting data {@link MeetingDto}
   * @throws NotFoundException if the indicated meeting doesn't exist
   * @throws ForbiddenException if the user isn't a meeting room member
   */
  MeetingDto getMeetingById(UUID meetingId, UserPrincipal currentUser);

  /**
   * Gets meeting by identifier. This method returns an entity because it's intended to be used only
   * to be called by services.
   *
   * @param meetingId meeting identifier {@link UUID}
   * @return The requested meeting {@link Meeting}
   */
  Optional<Meeting> getMeetingEntity(UUID meetingId);

  /**
   * Gets meeting by identifier for associated room. This method returns an entity because it's
   * intended to be used only to be called by services.
   *
   * @param roomId room identifier {@link UUID}
   * @return The requested meeting {@link Meeting} wrapped in a {@link Optional}
   */
  Optional<Meeting> getMeetingEntityByRoomId(UUID roomId);

  /**
   * Gets the meeting associated to indicated room
   *
   * @param roomId room identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The meeting of indicated room {@link MeetingDto}
   * @throws NotFoundException if the indicated room or the associated meeting don't exist
   * @throws ForbiddenException if the current user isn't a member of indicated room
   */
  MeetingDto getMeetingByRoomId(UUID roomId, UserPrincipal currentUser);

  /**
   * Deletes a meeting by identifier.
   *
   * @param meetingId identifier of meeting to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws ForbiddenException if the current user isn't a member of associated room
   */
  void deleteMeetingById(UUID meetingId, UserPrincipal currentUser);

  /**
   * Deletes a meeting. This method accepts entities because it's intended to be used only to be
   * called by services.
   *
   * @param meeting {@link Meeting} to delete
   * @param room meeting {@link Room}
   * @param userId current user identifier
   */
  void deleteMeeting(Meeting meeting, Room room, UUID userId);

  /**
   * Starts the recording on the specified meeting
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void startMeetingRecording(UUID meetingId, UserPrincipal currentUser);

  /**
   * Stops the recording on the specified meeting
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param recordingName the name used to save the recording on Files
   * @param folderId the folder id where the recording will be saved on Files
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void stopMeetingRecording(
      UUID meetingId, String recordingName, String folderId, UserPrincipal currentUser);
}
