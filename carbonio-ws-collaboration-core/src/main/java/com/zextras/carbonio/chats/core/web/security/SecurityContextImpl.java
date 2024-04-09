// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

public class SecurityContextImpl implements SecurityContext {

  private final UserPrincipal principal;

  public SecurityContextImpl(UserPrincipal principal) {
    this.principal = principal;
  }

  public static SecurityContextImpl create(UserPrincipal principal) {
    return new SecurityContextImpl(principal);
  }

  @Override
  public Principal getUserPrincipal() {
    return principal.getName() != null ? principal : null;
  }

  @Override
  public boolean isUserInRole(String role) {
    return true;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public String getAuthenticationScheme() {
    return null;
  }
}
