// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
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
    try (Transaction transaction = database.beginTransaction()) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
