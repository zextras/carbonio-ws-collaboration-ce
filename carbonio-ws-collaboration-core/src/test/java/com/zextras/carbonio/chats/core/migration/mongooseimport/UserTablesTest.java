// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class UserTablesTest {

  private static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");
  private static final String USER_A = "11111111-1111-4111-8111-111111111111";
  private static final String USER_B = "22222222-2222-4222-8222-222222222222";
  private static final String USER_C = "33333333-3333-4333-8333-333333333333";
  private static final String USER_D = "44444444-4444-4444-8444-444444444444";

  @Test
  @DisplayName("Last seen: non-positive skipped, future clamped to T0, non-UUID skipped, counted")
  void lastSeenBounds() throws SQLException {
    ResultSet rows = mock(ResultSet.class);
    when(rows.next()).thenReturn(true, true, true, true, true, false);
    when(rows.getString(1)).thenReturn(USER_A, USER_B, USER_C, "not-a-uuid", USER_D);
    when(rows.getLong(2)).thenReturn(0L, -5L, T0.getEpochSecond() + 86_400, 1_700_000_000L);
    Statement read = mock(Statement.class);
    when(read.executeQuery(anyString())).thenReturn(rows);
    Connection source = mock(Connection.class);
    when(source.createStatement()).thenReturn(read);
    PreparedStatement insert = mock(PreparedStatement.class);
    when(insert.executeBatch()).thenReturn(new int[] {1, 1});
    Connection target = mock(Connection.class);
    when(target.prepareStatement(anyString())).thenReturn(insert);
    ImportContext context = new ImportContext(T0);

    UserTables.Counts counts = UserTables.run(source, true, target, context);

    assertEquals(2, counts.lastSeen());
    verify(insert, never()).setString(eq(1), eq(USER_A));
    verify(insert, never()).setString(eq(1), eq(USER_B));
    verify(insert).setObject(2, T0.atOffset(ZoneOffset.UTC));
    verify(insert).setObject(2, Instant.ofEpochSecond(1_700_000_000L).atOffset(ZoneOffset.UTC));
    verify(insert, org.mockito.Mockito.times(2)).addBatch();
    assertEquals(2, context.of(SkipReason.LAST_SEEN_NONPOSITIVE));
    assertEquals(1, context.of(SkipReason.LAST_SEEN_FUTURE));
    assertEquals(1, context.of(SkipReason.LAST_SEEN_BAD_USER));
  }
}
