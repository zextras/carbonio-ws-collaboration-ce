package com.zextras.chats.core.repository;

import com.zextras.chats.core.data.entity.RoomUserSettings;
import java.util.Optional;

public interface RoomUserSettingsRepository {

  Optional<RoomUserSettings> getByRoomIdAndUserId(String roomId, String userId);

  void deleteByRoomId(String roomId);

}
