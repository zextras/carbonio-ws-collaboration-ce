package com.zextras.carbonio.chats.meeting.service;

import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import java.util.List;
import java.util.UUID;

public interface MeetingService {

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
   * Creates a meeting for the indicated room
   *
   * @param roomId      room identifier  {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The newly created meeting {@link MeetingDto}
   * @throws NotFoundException  if the indicated room doesn't exist
   * @throws ForbiddenException if the current user isn't a member of indicated room
   * @throws ConflictException  if the room meeting already exists
   */
  MeetingDto createMeetingByRoomId(UUID roomId, UserPrincipal currentUser);

}
