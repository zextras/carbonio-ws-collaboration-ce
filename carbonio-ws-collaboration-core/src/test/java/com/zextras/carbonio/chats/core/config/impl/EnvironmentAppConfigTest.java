// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class EnvironmentAppConfigTest {

  @Test
  @DisplayName("A duplicate environment key is rejected")
  void create_duplicateKeyThrows() {
    ConfigContribution first =
        new ConfigContribution() {
          @Override
          public Set<String> environmentKeys() {
            return Set.of("SAME_KEY");
          }
        };
    ConfigContribution second =
        new ConfigContribution() {
          @Override
          public Set<String> environmentKeys() {
            return Set.of("SAME_KEY");
          }
        };

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> EnvironmentAppConfig.create(List.of(first, second)));
    assertTrue(exception.getMessage().contains("SAME_KEY"));
  }
}
