// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class MamTime {

  private static final Instant MIN = Instant.parse("0001-01-01T00:00:00Z");
  private static final Instant MAX_EXCLUSIVE = Instant.parse("+10000-01-01T00:00:00Z");

  private MamTime() {}

  public static Instant instant(long mam) {
    return Instant.EPOCH.plus(mam >> 8, ChronoUnit.MICROS);
  }

  public static boolean inRange(Instant time) {
    return !time.isBefore(MIN) && time.isBefore(MAX_EXCLUSIVE);
  }

  public static Instant micros(long epochMicros) {
    return Instant.EPOCH.plus(epochMicros, ChronoUnit.MICROS);
  }
}
