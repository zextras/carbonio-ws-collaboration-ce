// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigName;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class CoreConfigContributionTest {

  @Test
  void serverThreadSettingsAreReadFromEnvironmentToo() {
    Set<String> env = new CoreConfigContribution().environmentKeys();
    assertTrue(env.contains(ConfigName.MAX_THREADS.name()));
    assertTrue(env.contains(ConfigName.MIN_THREADS.name()));
    assertTrue(env.contains(ConfigName.MAX_QUEUE_REQUESTS.name()));
  }
}
