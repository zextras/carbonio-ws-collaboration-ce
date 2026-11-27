// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.UserType;
import org.junit.jupiter.api.Test;

@UnitTest
class AllowAllUserAccessPolicyTest {

  @Test
  void allowsEveryone() {
    var policy = new AllowAllUserAccessPolicy();

    assertTrue(policy.isAllowed("id", UserType.INTERNAL, "rooms"));
    assertTrue(policy.isAllowed("id", UserType.GUEST, "/events"));
  }
}
