// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PluginApiTest {

  @Test
  void sameVersionIsCompatible() {
    assertThat(PluginApi.isCompatible("1.0", "1.0")).isTrue();
  }

  @Test
  void olderMinorRequiredIsCompatible() {
    assertThat(PluginApi.isCompatible("1.0", "1.3")).isTrue();
  }

  @Test
  void newerMinorRequiredIsNotCompatible() {
    assertThat(PluginApi.isCompatible("1.4", "1.3")).isFalse();
  }

  @Test
  void differentMajorIsNotCompatible() {
    assertThat(PluginApi.isCompatible("2.0", "1.0")).isFalse();
    assertThat(PluginApi.isCompatible("0.9", "1.0")).isFalse();
  }

  @Test
  void malformedVersionIsRejected() {
    assertThatThrownBy(() -> PluginApi.isCompatible("one", "1.0"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("one");
  }

  @Test
  void coreVersionIsOneZero() {
    assertThat(PluginApi.VERSION).isEqualTo("1.0");
  }
}
