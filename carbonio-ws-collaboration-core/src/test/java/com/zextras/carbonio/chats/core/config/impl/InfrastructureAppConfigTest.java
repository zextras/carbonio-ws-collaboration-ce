// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
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

    List<ConfigContribution> contributions = List.of(first, second);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> InfrastructureAppConfig.create(contributions));
    assertTrue(exception.getMessage().contains("SAME_KEY"));
  }

  private final ConfigContribution defaults =
      new ConfigContribution() {
        @Override
        public Map<String, String> infrastructureDefaults() {
          return Map.of("SERVICE_HOST", "127.78.0.4", "SERVICE_PORT", "10000");
        }
      };

  @Test
  @DisplayName("An environment variable named like the key overrides the default")
  void get_environmentOverridesDefault() {
    AppConfig config =
        InfrastructureAppConfig.create(
            List.of(defaults), Map.of("SERVICE_HOST", "0.0.0.0", "SERVICE_PORT", "8080")::get);

    assertEquals("0.0.0.0", config.get(String.class, "SERVICE_HOST").orElseThrow());
    assertEquals(8080, config.get(Integer.class, "SERVICE_PORT").orElseThrow());
  }

  @Test
  @DisplayName("Unset or blank environment variables keep the default")
  void get_missingOrBlankEnvironmentKeepsDefault() {
    AppConfig config =
        InfrastructureAppConfig.create(List.of(defaults), Map.of("SERVICE_HOST", " ")::get);

    assertEquals("127.78.0.4", config.get(String.class, "SERVICE_HOST").orElseThrow());
    assertEquals(10000, config.get(Integer.class, "SERVICE_PORT").orElseThrow());
  }
}
