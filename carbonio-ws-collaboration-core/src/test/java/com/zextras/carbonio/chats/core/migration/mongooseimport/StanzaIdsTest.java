// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class StanzaIdsTest {

  @Test
  @DisplayName("Round trips a real MAM id")
  void roundTrip() {
    long mam = 458597056046210817L;
    String encoded = StanzaIds.encode(mam);
    assertEquals("CNA3IUUAP7O1", encoded);
    assertEquals(OptionalLong.of(mam), StanzaIds.decode(encoded));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"not-a-stanza-id!", "ZZZZ", "cna3iuuap7o1", "+1", "", "0000000000000A", "0"})
  @DisplayName("Rejects values that are not a stanza id")
  void rejects(String value) {
    assertTrue(StanzaIds.decode(value).isEmpty());
  }

  @Test
  @DisplayName("Rejects null and 13-char values that overflow a long")
  void overflowAndNull() {
    assertTrue(StanzaIds.decode(null).isEmpty());
    assertTrue(StanzaIds.decode("VVVVVVVVVVVVV").isEmpty());
  }
}
