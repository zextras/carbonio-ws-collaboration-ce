// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zextras.carbonio.ws.plugin.api.PluginDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

class PluginRegistryTest {

  @Test
  void compatiblePluginsAreSortedById() {
    var result = PluginRegistry.validate(List.of(
        new PluginDescriptor("turn", "0.1.0", "1.0"),
        new PluginDescriptor("recording", "0.1.0", "1.0")));
    assertThat(result).extracting(PluginDescriptor::id).containsExactly("recording", "turn");
  }

  @Test
  void incompatiblePluginFailsWithAClearMessage() {
    assertThatThrownBy(() -> PluginRegistry.validate(List.of(new PluginDescriptor("x", "0.1.0", "2.0"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Plugin 'x' 0.1.0 requires plugin API 2.0 but core provides 1.0");
  }

  @Test
  void duplicateIdsAreRejected() {
    assertThatThrownBy(() -> PluginRegistry.validate(List.of(
            new PluginDescriptor("x", "0.1.0", "1.0"), new PluginDescriptor("x", "0.2.0", "1.0"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate plugin id 'x'");
  }
}
