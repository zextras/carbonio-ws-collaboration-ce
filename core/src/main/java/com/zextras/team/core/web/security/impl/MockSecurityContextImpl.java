package com.zextras.team.core.web.security.impl;

import com.zextras.team.core.web.security.MockSecurityContext;
import com.zextras.team.core.web.security.MockUserPrincipal;
import java.security.Principal;

public class MockSecurityContextImpl implements MockSecurityContext {

  @Override
  public Principal getUserPrincipal() {
    return new MockUserPrincipal("1", "one", "zextras");
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
