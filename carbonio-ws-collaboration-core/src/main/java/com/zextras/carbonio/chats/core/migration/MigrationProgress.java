// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

public final class MigrationProgress {

  private static volatile long lastProgressNanos = System.nanoTime();
  private static volatile String status = "Running database migrations";
  private static volatile long backendPid = -1;

  private MigrationProgress() {}

  public static void tick() {
    lastProgressNanos = System.nanoTime();
  }

  public static void tick(String newStatus) {
    status = newStatus;
    lastProgressNanos = System.nanoTime();
  }

  public static long lastProgressNanos() {
    return lastProgressNanos;
  }

  public static String status() {
    return status;
  }

  public static void backendPid(long pid) {
    backendPid = pid;
  }

  public static long backendPid() {
    return backendPid;
  }
}
