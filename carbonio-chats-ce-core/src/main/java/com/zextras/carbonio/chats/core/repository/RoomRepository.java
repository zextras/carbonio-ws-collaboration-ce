// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

  List<Room> getByUserId(String userId, boolean addSubscriptions);

  Optional<Room> getById(String id);

  Optional<Room> getByIdAndUserId(String roomId, String userId);

  Optional<Room> getOneToOneByAllUserIds(String user1Id, String user2Id);

  Room insert(Room room);

  Room update(Room room);

  void delete(String id);

  Optional<Integer> getChannelMaxRanksByWorkspace(String workspaceId);

}
