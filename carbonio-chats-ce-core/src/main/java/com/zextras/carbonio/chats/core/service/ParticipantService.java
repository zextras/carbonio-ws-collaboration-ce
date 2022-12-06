package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantService {

  /**
   * Inserts a participant into the meeting of indicated room. If the room meeting doesn't exist it created it
   *
   * @param roomId       room identifier of the associated meeting in which to insert the participant {@link UUID}
   * @param joinSettings participation settings {@link JoinSettingsDto}
   * @param currentUser  current authenticated user {@link UserPrincipal}
   * @return The newly meeting {@link MeetingDto} wrapped into an {@link Optional} only if it was created
   */
  Optional<MeetingDto> insertMeetingParticipantByRoomId(
    UUID roomId, JoinSettingsDto joinSettings, UserPrincipal currentUser
  );

  /**
   * Inserts a participant into the indicated meeting
   *
   * @param meetingId    identifier of the meeting in which to insert the participant {@link UUID}
   * @param joinSettings participation settings {@link JoinSettingsDto}
   * @param currentUser  current authenticated user {@link UserPrincipal}
   * @throws ConflictException  if the session is already inserted into the meeting
   * @throws NotFoundException  if the meeting doesn't exist
   * @throws NotFoundException  if the associated room doesn't exist
   * @throws ForbiddenException if the user isn't an associated room member
   * @throws ForbiddenException if the user isn't an associated room owner and mustBeRoomOwner is true
   */
  void insertMeetingParticipant(UUID meetingId, JoinSettingsDto joinSettings, UserPrincipal currentUser);

  /**
   * Removes the participant of current user from the indicated meeting
   *
   * @param meetingId   identifier of the meeting from which to remove the participant {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException  if the meeting doesn't exist
   * @throws NotFoundException  if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException  if the indicated room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   * @throws ForbiddenException if the user isn't a room owner and mustBeOwner is true
   */
  void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser);
}
