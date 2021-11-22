package com.zextras.team.core.repository.impl;

import com.zextras.team.core.data.entity.User;
import com.zextras.team.core.repository.UserRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;

@Transactional
public class EbeanUserRepository implements UserRepository {

  private final Database db;

  @Inject
  public EbeanUserRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<User> get() {
    return db.find(User.class).findList();
  }

  @Override
  public Optional<User> getWithRoomsById(String id) {
    return db.find(User.class)
      .fetch("subscriptions")
      .fetch("subscriptions.room")
      .where().eq("id", id)
      .findOneOrEmpty();
  }

  @Override
  public User save(User user) {
    if (user.getId() != null && db.find(User.class).findOneOrEmpty().isPresent()) {
      db.update(user);
    } else {
      user.setId(Optional.ofNullable(user.getId()).orElse(UUID.randomUUID().toString()));
      db.save(user);
    }
    return db.find(User.class, user.getId());
  }

  @Override
  public void delete(String id) {
    db.delete(User.class, id);
  }
}
