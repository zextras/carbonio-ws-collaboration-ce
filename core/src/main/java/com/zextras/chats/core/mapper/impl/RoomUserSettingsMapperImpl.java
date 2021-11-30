package com.zextras.chats.core.mapper.impl;

import com.zextras.chats.core.model.RoomUserSettingsDto;
import com.zextras.chats.core.data.entity.RoomUserSettings;
import com.zextras.chats.core.mapper.RoomUserSettingsMapper;
import java.util.List;

public class RoomUserSettingsMapperImpl implements RoomUserSettingsMapper {

  @Override
  public RoomUserSettingsDto ent2dto(RoomUserSettings roomUserSettings) {
    return RoomUserSettingsDto.create().isMuted(roomUserSettings != null && roomUserSettings.getMutedUntil() != null);
  }

  @Override
  public RoomUserSettingsDto ent2dto(List<RoomUserSettings> roomUserSettingsList, String userId) {
    return ent2dto(roomUserSettingsList == null ? null : roomUserSettingsList.stream().filter(settings -> settings.getUserId().equals(userId)).findAny().orElse(null));
  }
}
