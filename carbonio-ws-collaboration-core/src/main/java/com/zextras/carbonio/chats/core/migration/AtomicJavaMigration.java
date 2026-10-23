// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

import java.sql.Connection;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public abstract class AtomicJavaMigration extends BaseJavaMigration {

  protected abstract void doMigrate(Context context) throws Exception;

  @Override
  public final void migrate(Context context) throws Exception {
    try {
      doMigrate(context);
    } catch (Throwable t) {
      // Flyway rolls back only on Exception; on an Error its finally commits via setAutoCommit.
      abortTransaction(context.getConnection(), t);
      if (t instanceof Exception e) {
        throw e;
      }
      throw new FlywayException("Migration " + getVersion() + " aborted", t);
    }
  }

  private static void abortTransaction(Connection connection, Throwable cause) {
    try {
      connection.rollback();
    } catch (Throwable rollbackFailure) {
      cause.addSuppressed(rollbackFailure);
      try {
        connection.unwrap(Connection.class).close();
      } catch (Throwable closeFailure) {
        cause.addSuppressed(closeFailure);
      }
    }
  }
}
