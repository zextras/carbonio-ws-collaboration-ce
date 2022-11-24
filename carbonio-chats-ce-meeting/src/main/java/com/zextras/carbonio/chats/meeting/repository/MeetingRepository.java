package com.zextras.carbonio.chats.meeting.repository;

import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import java.util.Optional;

public interface MeetingRepository {

  /**
   * Gets a meeting by the identifier of his room
   *
   * @param roomId room identifier
   * @return requested meeting {@link Meeting}
   */
  Optional<Meeting> getMeetingByRoomId(String roomId);

  /**
   * Insert a new row into MEETING table
   *
   * @param meeting meeting to insert {@link Meeting}
   * @return meeting inserted {@link Meeting}
   */
  Meeting insert(Meeting meeting);
}
