package com.zextras.chats.core.mapper;

import com.zextras.chats.core.model.RoomUserSettingsDto;
import com.zextras.chats.core.data.entity.RoomUserSettings;
import java.util.List;

public interface RoomUserSettingsMapper {

  RoomUserSettingsDto ent2dto(RoomUserSettings roomUserSettings);

  RoomUserSettingsDto ent2dto(List<RoomUserSettings> roomUserSettingsList, String userId);



}