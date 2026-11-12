// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatsTest {

  @Test
  @DisplayName("A boot that throws an Exception exits with status 1")
  void runOrExit_exception_exitsWithOne() {
    List<Integer> exits = new ArrayList<>();

    Chats.runOrExit(
        () -> {
          throw new Exception("boom");
        },
        exits::add);

    assertEquals(List.of(1), exits);
  }

  @Test
  @DisplayName("A boot that throws an Error exits with status 1")
  void runOrExit_error_exitsWithOne() {
    List<Integer> exits = new ArrayList<>();

    Chats.runOrExit(
        () -> {
          throw new NoClassDefFoundError("missing");
        },
        exits::add);

    assertEquals(List.of(1), exits);
  }

  @Test
  @DisplayName("A successful boot does not exit")
  void runOrExit_success_doesNotExit() {
    List<Integer> exits = new ArrayList<>();
    List<String> ran = new ArrayList<>();

    Chats.runOrExit(() -> ran.add("boot"), exits::add);

    assertEquals(List.of("boot"), ran);
    assertTrue(exits.isEmpty());
  }
}
