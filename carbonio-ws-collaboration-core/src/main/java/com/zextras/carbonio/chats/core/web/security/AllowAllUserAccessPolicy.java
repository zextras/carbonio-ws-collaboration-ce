// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.data.type.UserType;

public class AllowAllUserAccessPolicy implements UserAccessPolicy {

  @Override
  public boolean isAllowed(String userId, UserType userType, String requestPath) {
    return true;
  }
}
