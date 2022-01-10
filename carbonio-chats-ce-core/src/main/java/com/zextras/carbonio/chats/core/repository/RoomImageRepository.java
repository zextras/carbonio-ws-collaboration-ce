package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.RoomImage;
import java.util.Optional;

public interface RoomImageRepository {

  Optional<RoomImage> getByRoomId(String roomId);

  RoomImage save(RoomImage roomImage);
}
