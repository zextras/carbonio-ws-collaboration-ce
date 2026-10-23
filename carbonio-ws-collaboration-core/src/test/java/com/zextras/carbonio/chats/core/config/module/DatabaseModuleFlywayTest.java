// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import java.lang.reflect.Method;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class DatabaseModuleFlywayTest {

  private static Flyway flyway() throws Exception {
    Method provider =
        DatabaseModule.class.getDeclaredMethod(
            "getFlyway", HikariDataSource.class, MessageDispatcherCredentials.class);
    provider.setAccessible(true);
    try (HikariDataSource dataSource = new HikariDataSource()) {
      return (Flyway)
          provider.invoke(
              new DatabaseModule(),
              dataSource,
              new MessageDispatcherCredentials("127.0.0.1", 1, null, null, null));
    }
  }

  @Test
  @DisplayName("All pending migrations run in one transaction: a failed upgrade applies nothing")
  void pendingMigrationsAreGrouped() throws Exception {
    assertTrue(flyway().getConfiguration().isGroup());
  }

  @Test
  @DisplayName("The lock is a session lock on the migration connection and other nodes wait for it")
  void lockIsHeldByTheMigrationConnection() throws Exception {
    Flyway flyway = flyway();
    Configuration configuration = flyway.getConfiguration();
    assertEquals(-1, configuration.getLockRetryCount());
    assertFalse(
        flyway
            .getConfigurationExtension(PostgreSQLConfigurationExtension.class)
            .isTransactionalLock());
  }
}
