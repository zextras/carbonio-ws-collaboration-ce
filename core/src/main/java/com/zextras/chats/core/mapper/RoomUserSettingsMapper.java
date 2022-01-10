package com.zextras.chats.core.mapper;

import com.zextras.chats.core.data.entity.RoomUserSettings;
import com.zextras.chats.core.model.RoomUserSettingsDto;
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