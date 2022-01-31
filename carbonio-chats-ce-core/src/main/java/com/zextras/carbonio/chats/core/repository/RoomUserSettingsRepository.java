// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import java.util.Optional;

public interface RoomUserSettingsRepository {

  Optional<RoomUserSettings> getByRoomIdAndUserId(String roomId, String userId);

  void deleteByRoomId(String roomId);

}
