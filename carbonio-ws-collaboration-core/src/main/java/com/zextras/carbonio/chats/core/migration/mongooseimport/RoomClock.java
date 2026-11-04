// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class RoomClock {

  private Instant first;
  private Instant last;

  public Instant next(Instant candidate) {
    Instant value = candidate;
    if (last != null && !candidate.isAfter(last)) {
      value = last.plus(1, ChronoUnit.MICROS);
    }
    if (first == null) {
      first = value;
    }
    last = value;
    return value;
  }

  @Nullable
  public Instant first() {
    return first;
  }
}
