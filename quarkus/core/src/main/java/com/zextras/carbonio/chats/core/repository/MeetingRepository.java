// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository {

  Optional<Meeting> getById(String meetingId);

  List<Meeting> getByRoomsIds(List<String> roomsIds);

  Optional<Meeting> getByRoomId(String roomId);

  Meeting insert(Meeting meeting);

  Meeting update(Meeting meeting);

  void delete(Meeting meeting);
}
