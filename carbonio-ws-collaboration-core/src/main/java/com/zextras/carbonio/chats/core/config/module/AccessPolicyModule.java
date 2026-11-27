// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.core.web.security.AllowAllUserAccessPolicy;
import com.zextras.carbonio.chats.core.web.security.UserAccessPolicy;

public class AccessPolicyModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(UserAccessPolicy.class).to(userAccessPolicy());
  }

  protected Class<? extends UserAccessPolicy> userAccessPolicy() {
    return AllowAllUserAccessPolicy.class;
  }
}
