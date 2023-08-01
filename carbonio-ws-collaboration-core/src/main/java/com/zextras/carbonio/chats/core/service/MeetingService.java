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
import com.zextras.carbonio.meeting.model.MeetingUserDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public interface MeetingService {

  /**
   * Creates a new meeting
   *
   * @param user
   * @param name
   * @param meetingType
   * @param roomId
   * @param users
   * @param expiration
   * @return {@link MeetingDto}
   */
  MeetingDto createMeeting(UserPrincipal user,
    String name,
    MeetingTypeDto meetingType,
    UUID roomId,
    List<MeetingUserDto> users,
    OffsetDateTime expiration);

  /**
   * Updates a meeting
   *
   * @param user
   * @param meetingId
   * @param started
   * @return {@link MeetingDto}
   */
  MeetingDto updateMeeting(UserPrincipal user, UUID meetingId, Boolean started);

  /**
   * Gets the meetings list of all rooms where the authenticated user is a member
   *
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The {@link List} of {@link MeetingDto} of all rooms where the authenticated user is a member
   */
  List<MeetingDto> getMeetings(UserPrincipal currentUser);

  /**
   * Gets indicated meeting data
   *
   * @param meetingId   meeting identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The indicated meeting data {@link MeetingDto}
   * @throws NotFoundException  if the indicated meeting doesn't exist
   * @throws ForbiddenException if the user isn't a meeting room member
   */
  MeetingDto getMeetingById(UUID meetingId, UserPrincipal currentUser);

  /**
   * Gets meeting by identifier. This method returns an entity because it's intended to be used only to be called by
   * services.
   *
   * @param meetingId meeting identifier {@link UUID}
   * @return The requested meeting {@link Meeting}
   */
  Optional<Meeting> getMeetingEntity(UUID meetingId);

  /**
   * Gets meeting by identifier for associated room. This method returns an entity because it's intended to be used only
   * to be called by services.
   *
   * @param roomId room identifier {@link UUID}
   * @return The requested meeting {@link Meeting} wrapped in a {@link Optional}
   */
  Optional<Meeting> getMeetingEntityByRoomId(UUID roomId);


  /**
   * Gets the meeting associated to indicated room
   *
   * @param roomId      room identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The meeting of indicated room {@link MeetingDto}
   * @throws NotFoundException  if the indicated room or the associated meeting don't exist
   * @throws ForbiddenException if the current user isn't a member of indicated room
   */
  MeetingDto getMeetingByRoomId(UUID roomId, UserPrincipal currentUser);

  /**
   * Gets or creates a meeting for the indicated room
   *
   * @param roomId      room identifier  {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The requested or newly created meeting {@link Meeting}
   * @throws NotFoundException  if the indicated room doesn't exist
   * @throws ForbiddenException if the current user isn't a member of indicated room
   */
  Meeting getsOrCreatesMeetingEntityByRoomId(UUID roomId, UserPrincipal currentUser);

  /**
   * Deletes a meeting by identifier.
   *
   * @param meetingId   identifier of meeting to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException  if the meeting doesn't exist
   * @throws ForbiddenException if the current user isn't a member of associated room
   */
  void deleteMeetingById(UUID meetingId, UserPrincipal currentUser);

  /**
   * Deletes a meeting. This method accepts entities because it's intended to be used only to be called by services.
   *
   * @param meeting   {@link Meeting} to delete
   * @param room      meeting {@link Room}
   * @param userId    current user identifier
   * @param sessionId current session identifier
   */
  void deleteMeeting(Meeting meeting, Room room, UUID userId, @Nullable String sessionId);

}
