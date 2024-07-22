// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.database.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import io.ebean.Database;

@Singleton
public class EbeanDatabaseInfoService implements DatabaseInfoService {

  private final Database database;

  @Inject
  public EbeanDatabaseInfoService(Database database) {
    this.database = database;
  }

  @Override
  public boolean isAlive() {
    return database.sqlQuery("SELECT 1 AS health_check").findOneOrEmpty().isPresent();
  }
}
