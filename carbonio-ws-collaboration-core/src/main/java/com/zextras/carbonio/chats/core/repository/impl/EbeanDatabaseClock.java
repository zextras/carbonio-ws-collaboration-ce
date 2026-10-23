// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.repository.DatabaseClock;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Singleton
public class EbeanDatabaseClock implements DatabaseClock {

  private final Database db;

  @Inject
  public EbeanDatabaseClock(Database db) {
    this.db = db;
  }

  @Override
  public OffsetDateTime now() {
    return db.sqlQuery("SELECT clock_timestamp() AS now")
        .findOne()
        .getTimestamp("now")
        .toInstant()
        .atOffset(ZoneOffset.UTC);
  }
}
