package com.zextras.carbonio.chats.meeting.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import java.util.UUID;

public interface MeetingService {

  /**
   * Creates a meeting for the indicated room
   *
   * @param roomId      room identifier  {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The newly created meeting {@link MeetingDto}
   */
  MeetingDto createMeetingByRoom(UUID roomId, UserPrincipal currentUser);

}
