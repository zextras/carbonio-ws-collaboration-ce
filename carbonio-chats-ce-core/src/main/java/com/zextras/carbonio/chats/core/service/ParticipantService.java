package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public interface ParticipantService {

  /**
   * Inserts a participant into the meeting of the specified room. If the meeting doesn’t exist it will be created
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

  /**
   * Removes the participant of the current user from a meeting. This method accepts entities because it's intended to
   * be used only to be called by services.
   *
   * @param meeting   participant {@link Meeting}
   * @param room      participant {@link Room}
   * @param userId    identifier of the user to remove
   * @param sessionId identifier of the session to remove. If it is null, it removes all user sessions
   */
  void removeMeetingParticipant(Meeting meeting, Room room, UUID userId, @Nullable String sessionId);

  /**
   * Sets the video stream enabling in the meeting for the current session
   *
   * @param meetingId        meeting identifier {@link UUID}
   * @param sessionId        identifier of the session to set video stream enabling
   * @param hasVideoStreamOn indicates whether the video stream must be enabled or not
   * @param currentUser      currentUser current authenticated user {@link UserPrincipal}
   */
  void enableVideoStream(UUID meetingId, String sessionId, boolean hasVideoStreamOn, UserPrincipal currentUser);

  /**
   * Sets the audio stream enabling in the meeting for the current session
   *
   * @param meetingId        meeting identifier {@link UUID}
   * @param sessionId        identifier of the session to set video stream enabling
   * @param hasAudioStreamOn indicates whether the audio stream must be enabled or not
   * @param currentUser      currentUser current authenticated user {@link UserPrincipal}
   */
  void enableAudioStream(UUID meetingId, String sessionId, boolean hasAudioStreamOn, UserPrincipal currentUser);
}
