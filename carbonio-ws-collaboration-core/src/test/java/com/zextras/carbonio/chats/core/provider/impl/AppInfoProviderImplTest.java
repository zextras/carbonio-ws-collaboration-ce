// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.provider.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.provider.AppInfoProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class AppInfoProviderImplTest {

  private final AppInfoProvider appInfoProvider;

  public AppInfoProviderImplTest() {
    this.appInfoProvider = new AppInfoProviderImpl();
  }

  @Test
  @DisplayName("Correctly gets ")
  void getVersion_testOk() {
    Optional<String> version = appInfoProvider.getVersion();
    assertTrue(version.isPresent());
    assertEquals("0.7.0", version.get());
  }
}
