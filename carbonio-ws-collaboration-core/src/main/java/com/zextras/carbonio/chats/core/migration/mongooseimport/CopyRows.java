// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;

final class CopyRows {

  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSS");

  private static final int INITIAL_BYTES = 1 << 16;
  private static final int RETAINED_BYTES = 1 << 22;

  private final String sql;
  private final Buffer buffer = new Buffer();

  private final StringBuilder row = new StringBuilder(512);
  private boolean firstField = true;
  private int rows;

  CopyRows(String table, String columns) {
    this.sql = "COPY " + table + " (" + columns + ") FROM STDIN";
  }

  CopyRows text(String value) {
    separate();
    if (value == null) {
      row.append("\\N");
      return this;
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\' -> row.append("\\\\");
        case '\n' -> row.append("\\n");
        case '\r' -> row.append("\\r");
        case '\t' -> row.append("\\t");
        default -> row.append(c);
      }
    }
    return this;
  }

  CopyRows number(long value) {
    separate();
    row.append(value);
    return this;
  }

  CopyRows nullableNumber(long value) {
    if (value <= 0) {
      separate();
      row.append("\\N");
      return this;
    }
    return number(value);
  }

  CopyRows bool(boolean value) {
    separate();
    row.append(value ? 't' : 'f');
    return this;
  }

  // Rounded to the microsecond like pgjdbc does for a bound LocalDateTime.
  CopyRows time(LocalDateTime value) {
    separate();
    if (value == null) {
      row.append("\\N");
      return this;
    }
    LocalDateTime rounded = value.getNano() % 1000 > 499 ? value.plusNanos(1000) : value;
    row.append(TIMESTAMP.format(rounded));
    return this;
  }

  void endRow() {
    row.append('\n');
    buffer.write(row.toString().getBytes(StandardCharsets.UTF_8));
    row.setLength(0);
    if (row.capacity() > RETAINED_BYTES) {
      row.trimToSize();
      row.ensureCapacity(512);
    }
    firstField = true;
    rows++;
  }

  int rows() {
    return rows;
  }

  void flush(Connection connection) throws SQLException {
    if (rows == 0) {
      return;
    }
    CopyIn copy = connection.unwrap(PGConnection.class).getCopyAPI().copyIn(sql);
    try {
      copy.writeToCopy(buffer.bytes(), 0, buffer.size());
      copy.endCopy();
    } finally {
      if (copy.isActive()) {
        copy.cancelCopy();
      }
    }
    clear();
  }

  String contents() {
    return new String(buffer.bytes(), 0, buffer.size(), StandardCharsets.UTF_8);
  }

  void clear() {
    buffer.reset();
    row.setLength(0);
    firstField = true;
    rows = 0;
  }

  private void separate() {
    if (!firstField) {
      row.append('\t');
    }
    firstField = false;
  }

  private static final class Buffer {
    private byte[] bytes = new byte[INITIAL_BYTES];
    private int size;

    byte[] bytes() {
      return bytes;
    }

    int size() {
      return size;
    }

    void write(byte[] data) {
      if (size + data.length > bytes.length) {
        bytes = Arrays.copyOf(bytes, Math.max(bytes.length * 2, size + data.length));
      }
      System.arraycopy(data, 0, bytes, size, data.length);
      size += data.length;
    }

    void reset() {
      size = 0;
      if (bytes.length > RETAINED_BYTES) {
        bytes = new byte[INITIAL_BYTES];
      }
    }
  }
}
