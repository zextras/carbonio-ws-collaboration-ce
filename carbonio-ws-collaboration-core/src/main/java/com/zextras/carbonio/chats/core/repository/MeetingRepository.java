// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository {

  /**
   * Gets a meeting by its identifier
   *
   * @param meetingId meeting identifier
   * @return requested meeting {@link Meeting}
   */
  Optional<Meeting> getById(String meetingId);

  /**
   * Gets a meeting list by their rooms identifiers
   *
   * @param roomsIds {@link List} of rooms identifiers
   * @return {@link Meeting} {@link List} of requested rooms
   */
  List<Meeting> getByRoomsIds(List<String> roomsIds);


  /**
   * Gets a meeting by the identifier of his room
   *
   * @param roomId room identifier
   * @return requested meeting {@link Meeting}
   */
  Optional<Meeting> getByRoomId(String roomId);

  /**
   * Inserts a new row into MEETING table
   *
   * @param name        name chosen for the meeting
   * @param meetingType {@link MeetingType} for the meeting
   * @param roomId      room identifier
   * @param expiration  {@link OffsetDateTime} expiration timestamp
   * @return meeting inserted {@link Meeting}
   */
  Meeting insert(String name, MeetingType meetingType, UUID roomId, OffsetDateTime expiration);

  /**
   * Inserts the {@link Meeting} data
   *
   * @param meeting {@link Meeting} modified
   * @return {@link Meeting} updated
   */
  Meeting update(Meeting meeting);

  /**
   * Deletes a meeting
   *
   * @param meeting meeting to delete
   */
  void delete(Meeting meeting);
}
