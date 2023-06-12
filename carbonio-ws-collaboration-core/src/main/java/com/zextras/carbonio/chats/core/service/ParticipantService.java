// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.StreamsDesiderataDto;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public interface ParticipantService {

  /**
   * Inserts a participant into the meeting of the specified room. If the meeting doesn’t exist it will be created
   *
   * @param roomId            room identifier of the associated meeting in which to insert the participant {@link UUID}
   * @param streamsDesiderata participation streams desiderata {@link StreamsDesiderataDto}
   * @param currentUser       current authenticated user {@link UserPrincipal}
   * @return The newly meeting {@link MeetingDto} wrapped into an {@link Optional} only if it was created
   */
  Optional<MeetingDto> insertMeetingParticipantByRoomId(
    UUID roomId, StreamsDesiderataDto streamsDesiderata, UserPrincipal currentUser
  );

  /**
   * Inserts a participant into the indicated meeting
   *
   * @param meetingId         identifier of the meeting in which to insert the participant {@link UUID}
   * @param streamsDesiderata participation streams desiderata  {@link StreamsDesiderataDto}
   * @param currentUser       current authenticated user {@link UserPrincipal}
   * @throws ConflictException  if the session is already inserted into the meeting
   * @throws NotFoundException  if the meeting doesn't exist
   * @throws NotFoundException  if the associated room doesn't exist
   * @throws ForbiddenException if the user isn't an associated room member
   * @throws ForbiddenException if the user isn't an associated room owner and mustBeRoomOwner is true
   */
  void insertMeetingParticipant(UUID meetingId, StreamsDesiderataDto streamsDesiderata, UserPrincipal currentUser);

  /**
   * Removes the participant of current user from the indicated meeting
   *
   * @param meetingId   identifier of the meeting from which to remove the participant {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException  if the meeting doesn't exist
   * @throws NotFoundException  if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException  if the associated room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   * @throws ForbiddenException if the user isn't a room owner and mustBeOwner is true
   */
  void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser);

  /**
   * Removes the participant of the current user from a meeting. This method accepts entities because it's intended to
   * be used only to be called by services.
   *
   * @param meeting   participant {@link Meeting}
   * @param room      participant {@link Room}
   * @param userId    identifier of the user to remove
   * @param sessionId identifier of the session to remove. If it is null, it removes all user sessions
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   */
  void removeMeetingParticipant(Meeting meeting, Room room, UUID userId, @Nullable String sessionId);

  /**
   * Updates the video stream status in the meeting for the current session
   *
   * @param meetingId   meeting identifier {@link UUID}
   * @param sessionId   identifier of the session whose video stream status has to be updated
   * @param enabled     indicates whether the video stream must be enabled or not
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException   if the meeting doesn't exist
   * @throws NotFoundException   if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException   if the associated room doesn't exist
   * @throws BadRequestException if another session tries to enable the stream
   * @throws ForbiddenException  if the current user isn't a room owner
   */
  void updateVideoStream(UUID meetingId, String sessionId, boolean enabled, UserPrincipal currentUser);

  /**
   * Updates the audio stream status in the meeting for the current session
   *
   * @param meetingId   meeting identifier {@link UUID}
   * @param sessionId   identifier of the session whose audio stream status has to be updated
   * @param enabled     indicates whether the audio stream must be enabled or not
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException   if the meeting doesn't exist
   * @throws NotFoundException   if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException   if the associated room doesn't exist
   * @throws BadRequestException if another session tries to enable the stream
   * @throws ForbiddenException  if the current user isn't a room owner
   */
  void updateAudioStream(UUID meetingId, String sessionId, boolean enabled, UserPrincipal currentUser);

  /**
   * Updates the screen stream status in the meeting for the current session
   *
   * @param meetingId   meeting identifier {@link UUID}
   * @param sessionId   identifier of the session whose screen stream status has to updated
   * @param enabled     indicates whether the video stream must be enabled or not
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException   if the meeting doesn't exist
   * @throws NotFoundException   if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException   if the associated room doesn't exist
   * @throws BadRequestException if another session tries to enable the stream
   * @throws ForbiddenException  if the current user isn't a room owner
   */
  void updateScreenStream(UUID meetingId, String sessionId, boolean enabled, UserPrincipal currentUser);
}
