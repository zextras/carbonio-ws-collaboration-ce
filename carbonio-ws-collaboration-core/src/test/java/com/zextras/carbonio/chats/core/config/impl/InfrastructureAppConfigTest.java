// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class InfrastructureAppConfigTest {

  @Test
  @DisplayName("A duplicate infrastructure default is rejected")
  void create_duplicateKeyThrows() {
    ConfigContribution first =
        new ConfigContribution() {
          @Override
          public Map<String, String> infrastructureDefaults() {
            return Map.of("SAME_KEY", "value");
          }
        };
    ConfigContribution second =
        new ConfigContribution() {
          @Override
          public Map<String, String> infrastructureDefaults() {
            return Map.of("SAME_KEY", "value");
          }
        };

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> InfrastructureAppConfig.create(List.of(first, second)));
    assertTrue(exception.getMessage().contains("SAME_KEY"));
  }
}
