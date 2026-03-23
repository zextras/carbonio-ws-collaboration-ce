// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.type;

public final class Size {

  private final long bytes;

  private Size(long bytes) {
    this.bytes = bytes;
  }

  public static Size of(long value, SizeUnit unit) {
    return new Size(unit.toBytes(value));
  }

  public static Size fromBytes(long bytes) {
    return new Size(bytes);
  }

  public long toBytes() {
    return bytes;
  }

  // Integer conversion
  public long to(SizeUnit unit) {
    return unit.fromBytes(bytes);
  }

  // Precise conversion
  public double toDouble(SizeUnit unit) {
    return unit.fromBytesDouble(bytes);
  }
}
