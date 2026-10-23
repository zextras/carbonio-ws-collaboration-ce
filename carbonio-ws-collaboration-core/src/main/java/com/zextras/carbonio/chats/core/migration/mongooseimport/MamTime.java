// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class MamTime {

  private MamTime() {}

  public static Instant instant(long mam) {
    return Instant.EPOCH.plus(mam >> 8, ChronoUnit.MICROS);
  }

  public static LocalDateTime millis(long mam, ZoneId zone) {
    return LocalDateTime.ofInstant(instant(mam).truncatedTo(ChronoUnit.MILLIS), zone);
  }

  public static boolean inRange(LocalDateTime time) {
    return time.getYear() >= 1 && time.getYear() <= 9999;
  }

  public static LocalDateTime micros(long epochMicros, ZoneId zone) {
    return LocalDateTime.ofInstant(Instant.EPOCH.plus(epochMicros, ChronoUnit.MICROS), zone);
  }
}
