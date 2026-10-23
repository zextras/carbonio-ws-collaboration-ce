// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public final class StanzaIds {

  private static final Pattern FORMAT = Pattern.compile("[0-9A-V]{1,13}");

  private StanzaIds() {}

  public static String encode(long mam) {
    return Long.toString(mam, 32).toUpperCase(Locale.ROOT);
  }

  public static OptionalLong decode(String s) {
    if (s == null || !FORMAT.matcher(s).matches()) {
      return OptionalLong.empty();
    }
    try {
      long value = Long.parseLong(s, 32);
      return value > 0 ? OptionalLong.of(value) : OptionalLong.empty();
    } catch (NumberFormatException e) {
      return OptionalLong.empty();
    }
  }
}
