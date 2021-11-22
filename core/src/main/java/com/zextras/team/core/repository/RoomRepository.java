package com.zextras.team.core.repository;

import com.zextras.team.core.data.entity.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

  List<Room> getByUserId(String userId);

  Optional<Room> getById(String id);

  Room save(Room room);

  void delete(String id);

}
