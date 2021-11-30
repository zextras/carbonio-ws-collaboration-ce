package com.zextras.chats.core.repository;

import com.zextras.chats.core.data.entity.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

  List<Room> getByUserId(String userId);

  Optional<Room> getById(String id);

  Optional<Room> getByIdAndUserId(String roomId, String userId);

  void save(Room room);

  void insert(Room room);

  void update(Room room);

  void delete(String id);

}
