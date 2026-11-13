// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

public class MigrationProgressCallback implements Callback {

  @Override
  public boolean supports(Event event, Context context) {
    return event == Event.BEFORE_MIGRATE
        || event == Event.BEFORE_EACH_MIGRATE
        || event == Event.AFTER_EACH_MIGRATE;
  }

  @Override
  public boolean canHandleInTransaction(Event event, Context context) {
    return true;
  }

  @Override
  public void handle(Event event, Context context) {
    if (event == Event.BEFORE_MIGRATE) {
      recordBackendPid(context);
      MigrationProgress.tick("Starting database migrations");
      return;
    }
    MigrationInfo info = context.getMigrationInfo();
    String migration =
        info == null ? "migration" : info.getVersion() + " (" + info.getDescription() + ")";
    MigrationProgress.tick(
        (event == Event.BEFORE_EACH_MIGRATE ? "Migrating " : "Migrated ") + migration);
  }

  @Override
  public String getCallbackName() {
    return "WscMigrationProgress";
  }

  private static void recordBackendPid(Context context) {
    try (Statement statement = context.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT pg_backend_pid()")) {
      if (rs.next()) {
        MigrationProgress.backendPid(rs.getLong(1));
      }
    } catch (Exception e) {
      ChatsLogger.debug(
          MigrationProgressCallback.class, "Cannot read the migration backend pid: " + e);
    }
  }
}
