package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import io.ebean.Database;
import java.util.Optional;
import javax.inject.Inject;

public class EbeanUserRepository implements UserRepository {

  private final Database database;

  @Inject
  public EbeanUserRepository(Database database) {

    this.database = database;
  }

  @Override
  public Optional<User> getById(String id) {
    return database.find(User.class)
      .where()
      .eq("id", id)
      .findOneOrEmpty();
  }
}
