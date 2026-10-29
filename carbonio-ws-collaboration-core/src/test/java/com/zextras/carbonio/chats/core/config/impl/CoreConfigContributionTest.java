// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class CoreConfigContributionTest {

  private final CoreConfigContribution contribution = new CoreConfigContribution();

  @Test
  @DisplayName("Simulcast tier heights are read from their Consul KV keys")
  void simulcastHeightsHaveConsulKeys() {
    Map<String, String> mappings = contribution.consulKvMappings();

    assertEquals(
        "carbonio-ws-collaboration/meeting/simulcast-high-height",
        mappings.get("VIDEO_SIMULCAST_HIGH_HEIGHT"));
    assertEquals(
        "carbonio-ws-collaboration/meeting/simulcast-medium-height",
        mappings.get("VIDEO_SIMULCAST_MEDIUM_HEIGHT"));
    assertEquals(
        "carbonio-ws-collaboration/meeting/simulcast-low-height",
        mappings.get("VIDEO_SIMULCAST_LOW_HEIGHT"));
  }

  @Test
  @DisplayName("Simulcast tier heights can be overridden from the environment")
  void simulcastHeightsAreEnvironmentKeys() {
    assertTrue(
        contribution
            .environmentKeys()
            .containsAll(
                Set.of(
                    "VIDEO_SIMULCAST_HIGH_HEIGHT",
                    "VIDEO_SIMULCAST_MEDIUM_HEIGHT",
                    "VIDEO_SIMULCAST_LOW_HEIGHT")));
  }
}
