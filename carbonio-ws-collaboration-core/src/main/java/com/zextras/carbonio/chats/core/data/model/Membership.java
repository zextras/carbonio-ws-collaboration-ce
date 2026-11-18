// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

public record Membership(boolean owner, @Nullable OffsetDateTime clearedAt) {

  public VisibilityFloor floor() {
    return new VisibilityFloor(clearedAt);
  }
}
