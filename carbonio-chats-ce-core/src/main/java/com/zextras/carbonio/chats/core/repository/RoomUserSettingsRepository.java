// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RoomUserSettingsRepository {

  Optional<RoomUserSettings> getByRoomIdAndUserId(String roomId, String userId);

  List<RoomUserSettings> getByUserId(String userId);

  List<RoomUserSettings> getByRoomId(String roomId);

  void delete(RoomUserSettings userSettings);

  RoomUserSettings save(RoomUserSettings roomUserSettings);

  void save(Collection<RoomUserSettings> roomUserSettingsList);

  Optional<Integer> getWorkspaceMaxRank(String userId);

  Map<String, RoomUserSettings> getWorkspaceMaxRanksMapByUsers(List<String> userIds);

  Map<String, RoomUserSettings> getWorkspaceMapByRoomId(String userId);
}
