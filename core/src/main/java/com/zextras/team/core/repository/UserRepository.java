package com.zextras.team.core.repository;

import com.zextras.team.core.data.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  List<User> get();

  Optional<User> getWithRoomsById(String id);

  User save(User user);

  void delete(String id);

}
