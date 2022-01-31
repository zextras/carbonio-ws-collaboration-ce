// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security.impl;

import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.MockSecurityContext;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

public class MockSecurityContextImpl implements MockSecurityContext {

  @Override
  public Optional<Principal> getUserPrincipal() {
    return Optional.of(new MockUserPrincipal(UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723"), "Snoopy", false));
  }

  @Override
  public boolean isUserInRole(String role) {
    return isSecure() && findRole(role);
  }

  private boolean findRole(String role) {
    return true;
  }

  @Override
  public boolean isSecure() {
    return true;
  }

  @Override
  public String getAuthenticationScheme() {
    return "BASIC";
  }

  @Override
  public UUID getUserPrincipalId() {
    return ((MockUserPrincipal) getUserPrincipal()
      .orElseThrow(UnauthorizedException::new)).getId();
  }
}
