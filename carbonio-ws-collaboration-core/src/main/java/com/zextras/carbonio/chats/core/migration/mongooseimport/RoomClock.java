// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class RoomClock {

  private LocalDateTime first;
  private LocalDateTime last;

  public LocalDateTime next(LocalDateTime candidate) {
    LocalDateTime value = candidate;
    if (last != null && !candidate.isAfter(last)) {
      value = last.plus(1, ChronoUnit.MILLIS);
    }
    if (first == null) {
      first = value;
    }
    last = value;
    return value;
  }

  @Nullable
  public LocalDateTime first() {
    return first;
  }
}
