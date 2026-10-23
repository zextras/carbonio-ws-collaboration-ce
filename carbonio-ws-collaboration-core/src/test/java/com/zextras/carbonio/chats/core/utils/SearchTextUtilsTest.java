// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class SearchTextUtilsTest {

  @Test
  @DisplayName("Lowercases, splits on runs of non-word characters")
  void normalizes() {
    assertEquals(
        List.of("hello", "world", "a_b", "perché"),
        SearchTextUtils.toSearchWords("Hello,World!  a_b (perché) #"));
  }

  @Test
  @DisplayName("Apostrophes, at-signs and dots split words")
  void symbolsSplit() {
    assertEquals(List.of("well", "known"), SearchTextUtils.toSearchWords("well-known"));
    assertEquals(List.of("100"), SearchTextUtils.toSearchWords("100%"));
    assertEquals(List.of("l", "ultimo"), SearchTextUtils.toSearchWords("l'ultimo"));
    assertEquals(
        List.of("mario", "zextras", "com"), SearchTextUtils.toSearchWords("mario@zextras.com"));
  }

  @Test
  @DisplayName("Only punctuation or blank gives no words")
  void noWords() {
    assertEquals(List.of(), SearchTextUtils.toSearchWords(",.;:-?! %"));
    assertEquals(List.of(), SearchTextUtils.toSearchWords(""));
    assertEquals(List.of(), SearchTextUtils.toSearchWords(null));
  }

  @Test
  @DisplayName("Escapes LIKE metacharacters")
  void escapes() {
    assertEquals("a\\_b\\%c\\\\", SearchTextUtils.escapeLike("a_b%c\\"));
  }
}
