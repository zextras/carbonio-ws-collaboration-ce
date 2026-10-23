// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

public record VisibilityFloor(@Nullable OffsetDateTime clearedAt) {

  public boolean canAccess(OffsetDateTime createdAt) {
    return clearedAt == null || createdAt.isAfter(clearedAt);
  }

  @Nullable
  public OffsetDateTime clamp(@Nullable OffsetDateTime requestedAfter) {
    if (clearedAt == null) {
      return requestedAfter;
    }
    return requestedAfter != null && requestedAfter.isAfter(clearedAt) ? requestedAfter : clearedAt;
  }
}
