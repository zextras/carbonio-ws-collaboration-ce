// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.sql.Connection;
import java.sql.SQLException;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class AtomicJavaMigrationTest {

  private Connection connection;
  private Context context;

  @BeforeEach
  void setUp() {
    connection = mock(Connection.class);
    context = mock(Context.class);
    when(context.getConnection()).thenReturn(connection);
  }

  @Test
  @DisplayName("Does not roll back when the migration completes")
  void success() throws Exception {
    new V9_9_9__test_migration(() -> {}).migrate(context);

    verify(connection, never()).rollback();
  }

  @Test
  @DisplayName("Rolls back and rethrows the same exception")
  void exception() throws Exception {
    IllegalStateException failure = new IllegalStateException("boom");

    Exception thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                new V9_9_9__test_migration(
                        () -> {
                          throw failure;
                        })
                    .migrate(context));

    assertSame(failure, thrown);
    verify(connection).rollback();
  }

  @Test
  @DisplayName("Rolls back on an Error and rethrows it as an exception")
  void error() throws Exception {
    OutOfMemoryError failure = new OutOfMemoryError("heap");

    FlywayException thrown =
        assertThrows(
            FlywayException.class,
            () ->
                new V9_9_9__test_migration(
                        () -> {
                          throw failure;
                        })
                    .migrate(context));

    assertSame(failure, thrown.getCause());
    verify(connection).rollback();
  }

  @Test
  @DisplayName("Closes the physical connection when the rollback fails")
  void rollbackFailure() throws Exception {
    Connection physical = mock(Connection.class);
    SQLException rollbackFailure = new SQLException("broken protocol");
    doThrow(rollbackFailure).when(connection).rollback();
    when(connection.unwrap(Connection.class)).thenReturn(physical);
    StackOverflowError failure = new StackOverflowError();

    FlywayException thrown =
        assertThrows(
            FlywayException.class,
            () ->
                new V9_9_9__test_migration(
                        () -> {
                          throw failure;
                        })
                    .migrate(context));

    assertSame(failure, thrown.getCause());
    assertEquals(rollbackFailure, failure.getSuppressed()[0]);
    verify(physical).close();
  }

  @FunctionalInterface
  private interface Body {
    void run() throws Exception;
  }

  private static class V9_9_9__test_migration extends AtomicJavaMigration {

    private final Body body;

    V9_9_9__test_migration(Body body) {
      this.body = body;
    }

    @Override
    protected void doMigrate(Context context) throws Exception {
      body.run();
    }
  }
}
