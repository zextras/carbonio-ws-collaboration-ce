// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
public class RoomUserSettingsMapper {

  public RoomUserSettingsDto ent2dto(@Nullable RoomUserSettings roomUserSettings) {
    return RoomUserSettingsDto.create().muted(roomUserSettings != null && roomUserSettings.getMutedUntil() != null);
  }

  public RoomUserSettingsDto ent2dto(List<RoomUserSettings> roomUserSettingsList) {
    return ent2dto(roomUserSettingsList != null && roomUserSettingsList.size() > 0 ?
      roomUserSettingsList.get(0) : null);
  }
}