// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

public interface MockSecurityContext {

  Optional<Principal> getUserPrincipal();

  boolean isUserInRole(String var1);

  boolean isSecure();

  String getAuthenticationScheme();

  UUID getUserPrincipalId();

}
