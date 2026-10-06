// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.type;

public enum SizeUnit {
  BYTES(1L),
  KB(1024L),
  MB(1024L * 1024),
  GB(1024L * 1024 * 1024);

  private final long multiplier;

  SizeUnit(long multiplier) {
    this.multiplier = multiplier;
  }

  public long toBytes(long value) {
    return value * multiplier;
  }

  public long fromBytes(long bytes) {
    return bytes / multiplier;
  }

  public double fromBytesDouble(long bytes) {
    return (double) bytes / multiplier;
  }

  public long getMultiplier() {
    return multiplier;
  }
}
