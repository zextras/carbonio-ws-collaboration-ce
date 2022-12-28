// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Transactional
@Singleton
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

  @Override
  public User save(User user) {
    database.save(user);
    return user;
  }
}
