// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanUserRepository implements UserRepository {

  private final Database database;

  @Inject
  public EbeanUserRepository(Database database) {
    this.database = database;
  }

  @Override
  public Optional<User> getById(String id) {
    return database.find(User.class).where().eq("id", id).findOneOrEmpty();
  }

  @Override
  public List<User> getByIds(List<String> ids) {
    return database.find(User.class).where().in("id", ids).findList();
  }

  @Override
  public User save(User user) {
    database.save(user);
    return user;
  }
}
