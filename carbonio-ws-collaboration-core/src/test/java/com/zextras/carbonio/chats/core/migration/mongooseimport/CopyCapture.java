// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;

final class CopyCapture {

  record Copy(String sql, String payload) {

    int rows() {
      return payload.isEmpty() ? 0 : payload.split("\n", -1).length - 1;
    }
  }

  private final Connection connection = mock(Connection.class);
  private final List<Copy> copies = new ArrayList<>();
  private SQLException failure;

  CopyCapture() throws SQLException {
    PGConnection postgres = mock(PGConnection.class);
    CopyManager manager = mock(CopyManager.class);
    when(connection.unwrap(PGConnection.class)).thenReturn(postgres);
    when(connection.createStatement()).thenReturn(mock(Statement.class));
    when(postgres.getCopyAPI()).thenReturn(manager);
    when(manager.copyIn(anyString()))
        .thenAnswer(
            invocation -> {
              String sql = invocation.getArgument(0);
              ByteArrayOutputStream payload = new ByteArrayOutputStream();
              CopyIn copy = mock(CopyIn.class);
              when(copy.isActive()).thenReturn(false);
              doAnswer(
                      write -> {
                        payload.write(
                            write.getArgument(0), write.getArgument(1), write.getArgument(2));
                        return null;
                      })
                  .when(copy)
                  .writeToCopy(any(byte[].class), anyInt(), anyInt());
              when(copy.endCopy())
                  .thenAnswer(
                      end -> {
                        if (failure != null) {
                          throw failure;
                        }
                        copies.add(new Copy(sql, payload.toString(StandardCharsets.UTF_8)));
                        return 0L;
                      });
              return copy;
            });
  }

  Connection connection() {
    return connection;
  }

  List<Copy> copies() {
    return copies;
  }

  void failNextCopies(SQLException failure) {
    this.failure = failure;
  }
}
