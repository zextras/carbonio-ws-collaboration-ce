// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserPrincipalTest {

  @Test
  void guestTypeIsMappedCaseInsensitively() {
    assertThat(UserType.from("guest")).isEqualTo(UserType.GUEST);
    assertThat(UserType.from("INTERNAL")).isEqualTo(UserType.INTERNAL);
    assertThat(UserType.from(null)).isEqualTo(UserType.INTERNAL);
    assertThat(UserType.from("whatever")).isEqualTo(UserType.INTERNAL);
  }

  @Test
  void hasAttributeIsTrueOnlyForTrueValues() {
    UserPrincipal user =
        new UserPrincipal(
            UUID.randomUUID(), UserType.INTERNAL, "a@b.c", "A", "tok", null,
            Map.of("carbonioWscRecordingEnabled", "TRUE", "other", "false"));
    assertThat(user.hasAttribute("carbonioWscRecordingEnabled")).isTrue();
    assertThat(user.hasAttribute("other")).isFalse();
    assertThat(user.hasAttribute("missing")).isFalse();
    assertThat(user.isGuest()).isFalse();
  }
}
