// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.database.impl;

import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import io.ebean.Database;
import io.ebean.Transaction;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EbeanDatabaseInfoService implements DatabaseInfoService {

  private final Database database;

  @Inject
  public EbeanDatabaseInfoService(Database database) {
    this.database = database;
  }

  @Override
  public boolean isAlive() {
    //If the statement in the try-with-resources throws an exception, it will be caught.
    try (Transaction transaction = database.beginTransaction()) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
