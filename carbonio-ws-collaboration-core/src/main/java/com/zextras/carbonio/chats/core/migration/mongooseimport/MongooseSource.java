// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public final class MongooseSource {

  public sealed interface Result permits NoOp, Ready {}

  public record NoOp(String reason) implements Result {}

  public record Ready(Connection connection, boolean hasPins, boolean hasMarkers, boolean hasLast)
      implements Result {}

  private static final String DATABASE_ABSENT = "3D000";

  private MongooseSource() {}

  public static Result open(MessageDispatcherCredentials credentials) {
    String name = credentials.getName();
    String username = credentials.getUsername();
    String password = credentials.getPassword();
    if (name == null && username == null && password == null) {
      return new NoOp("no dispatcher credentials");
    }
    if (name == null || username == null || password == null) {
      throw new IllegalStateException(
          "MongooseIM import failed: incomplete dispatcher credentials");
    }
    Properties props = new Properties();
    props.setProperty("user", username);
    props.setProperty("password", password);
    props.setProperty("connectTimeout", "10");
    props.setProperty("loginTimeout", "30");
    props.setProperty("sslmode", "disable");
    Connection connection;
    try {
      connection = DriverManager.getConnection(credentials.getJdbcUrl(), props);
    } catch (SQLException e) {
      if (DATABASE_ABSENT.equals(e.getSQLState())) {
        return new NoOp("dispatcher database absent");
      }
      throw failure("cannot connect to the dispatcher database", e);
    }
    boolean ready = false;
    try {
      Result result = probe(connection);
      ready = result instanceof Ready;
      return result;
    } catch (SQLException e) {
      throw failure("cannot read the dispatcher database", e);
    } finally {
      if (!ready) {
        close(connection);
      }
    }
  }

  public static void close(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      ChatsLogger.warn(MongooseSource.class, "MongooseIM import: source connection close failed");
    }
  }

  private static Result probe(Connection connection) throws SQLException {
    connection.setAutoCommit(false);
    connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    connection.setReadOnly(true);
    boolean users = exists(connection, "mam_server_user");
    boolean messages = exists(connection, "mam_muc_message");
    if (!users && !messages) {
      return new NoOp("no MongooseIM tables");
    }
    if (users != messages) {
      String missing = users ? "mam_muc_message" : "mam_server_user";
      throw new IllegalStateException("MongooseIM schema incomplete: missing " + missing);
    }
    warnOnForeignSessions(connection);
    return new Ready(
        connection,
        exists(connection, "pin_message"),
        exists(connection, "smart_markers"),
        exists(connection, "last"));
  }

  private static boolean exists(Connection connection, String table) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
      ps.setString(1, "public." + table);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  private static void warnOnForeignSessions(Connection connection) throws SQLException {
    try (PreparedStatement ps =
            connection.prepareStatement(
                "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"
                    + " AND pid <> pg_backend_pid()");
        ResultSet rs = ps.executeQuery()) {
      if (rs.next() && rs.getLong(1) > 0) {
        ChatsLogger.warn(
            MongooseSource.class,
            "MongooseIM import: other sessions are connected to the MongooseIM database,"
                + " the snapshot may not be quiescent");
      }
    }
  }

  private static IllegalStateException failure(String what, SQLException e) {
    return new IllegalStateException(
        "MongooseIM import failed: " + what + ", SQLState " + e.getSQLState());
  }
}
