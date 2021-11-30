package com.zextras.chats.core.web.security.impl;

import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.MockUserPrincipal;
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
}
