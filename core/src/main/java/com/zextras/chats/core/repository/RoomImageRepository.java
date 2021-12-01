package com.zextras.chats.core.repository;

import com.zextras.chats.core.data.entity.RoomImage;
import java.util.Optional;

public interface RoomImageRepository {

  Optional<RoomImage> getByRoomId(String roomId);

  void save(RoomImage roomImage);
}
