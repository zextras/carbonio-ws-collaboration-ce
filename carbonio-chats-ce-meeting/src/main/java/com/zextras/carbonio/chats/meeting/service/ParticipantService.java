package com.zextras.carbonio.chats.meeting.service;

import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.model.JoinSettingsDto;
import java.util.UUID;

public interface ParticipantService {

  /**
   * Insert a participant into the indicated meeting
   *
   * @param meetingId    identifier of the meeting in which to insert the participant {@link UUID}
   * @param joinSettings participation settings {@link JoinSettingsDto}
   * @param currentUser  current authenticated user {@link UserPrincipal}
   * @throws ConflictException  if the session is already inserted into the meeting
   * @throws NotFoundException  if the associated room doesn't exist
   * @throws ForbiddenException if the user isn't an associated room member
   * @throws ForbiddenException if the user isn't an associated room owner and mustBeRoomOwner is true
   */
  void insertMeetingParticipant(UUID meetingId, JoinSettingsDto joinSettings, UserPrincipal currentUser);
}
