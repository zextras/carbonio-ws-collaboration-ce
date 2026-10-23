// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SearchTextUtils {

  private static final Pattern NON_WORD_RUN = Pattern.compile("[^\\p{L}\\p{N}_]+");

  private SearchTextUtils() {}

  public static List<String> toSearchWords(String query) {
    if (query == null) {
      return List.of();
    }
    return Arrays.stream(NON_WORD_RUN.split(query.toLowerCase(Locale.ROOT)))
        .filter(w -> !w.isEmpty())
        .toList();
  }

  public static String escapeLike(String word) {
    return word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
