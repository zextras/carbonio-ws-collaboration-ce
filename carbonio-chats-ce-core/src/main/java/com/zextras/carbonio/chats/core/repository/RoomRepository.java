package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

  List<Room> getByUserId(String userId);

  Optional<Room> getById(String id);

  Optional<Room> getByIdAndUserId(String roomId, String userId);

  Room insert(Room room);

  Room update(Room room);

  void delete(String id);

}
