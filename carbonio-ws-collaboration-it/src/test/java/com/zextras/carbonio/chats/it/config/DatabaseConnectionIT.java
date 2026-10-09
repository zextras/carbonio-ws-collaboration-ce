// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import io.ebean.Database;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class DatabaseConnectionIT {

  private final Database database;

  DatabaseConnectionIT(Database database) {
    this.database = database;
  }

  @Test
  void connectionsAreNamedForPgStatActivity() {
    assertEquals(
        "ws-collaboration",
        database
            .sqlQuery("select current_setting('application_name') as name")
            .findOne()
            .getString("name"));
  }
}
