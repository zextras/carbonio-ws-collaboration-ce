// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;


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
