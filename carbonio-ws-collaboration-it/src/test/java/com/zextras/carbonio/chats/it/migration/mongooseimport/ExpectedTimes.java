// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class ExpectedTimes {

  private ExpectedTimes() {}

  public static Instant mamInstant(long mamId) {
    long micros = mamId >> 8;
    return Instant.ofEpochSecond(micros / 1_000_000, (micros % 1_000_000) * 1_000);
  }

  public static LocalDateTime mamTime(long mamId) {
    return LocalDateTime.ofInstant(mamInstant(mamId), ZoneOffset.UTC);
  }
}
