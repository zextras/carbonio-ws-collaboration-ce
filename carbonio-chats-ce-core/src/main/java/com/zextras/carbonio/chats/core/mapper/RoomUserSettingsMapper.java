// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330")
public abstract class RoomUserSettingsMapper {

  @Mapping(target = "muted", expression = "java(roomUserSettings != null && roomUserSettings.getMutedUntil() != null)")
  public abstract RoomUserSettingsDto ent2dto(RoomUserSettings roomUserSettings);

  public RoomUserSettingsDto ent2dto(List<RoomUserSettings> roomUserSettingsList, String userId) {
    return ent2dto(roomUserSettingsList == null ? null :
      roomUserSettingsList.stream().filter(settings -> settings.getUserId().equals(userId)).findAny().orElse(null));
  }
}