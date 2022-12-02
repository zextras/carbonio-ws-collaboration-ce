package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository {

  /**
   * Gets a meeting by its identifier
   *
   * @param meetingId meeting identifier
   * @return requested meeting {@link Meeting}
   */
  Optional<Meeting> getMeetingById(String meetingId);

  /**
   * Gets a meeting list by their rooms identifiers
   *
   * @param roomsIds {@link List} of rooms identifiers
   * @return {@link Meeting} {@link List} of requested rooms
   */
  List<Meeting> getMeetingsByRoomsIds(List<String> roomsIds);


  /**
   * Gets a meeting by the identifier of his room
   *
   * @param roomId room identifier
   * @return requested meeting {@link Meeting}
   */
  Optional<Meeting> getMeetingByRoomId(String roomId);

  /**
   * Inserts a new row into MEETING table
   *
   * @param meeting meeting to insert {@link Meeting}
   * @return meeting inserted {@link Meeting}
   */
  Meeting insert(Meeting meeting);

  /**
   * Deletes a meeting by identifier
   *
   * @param meetingId meeting identifier
   */
  void deleteById(String meetingId);
}
