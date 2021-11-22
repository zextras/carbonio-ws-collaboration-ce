package com.zextras.team.core.web.security;

import java.security.Principal;

public interface MockSecurityContext {

  Principal getUserPrincipal();

  boolean isUserInRole(String var1);

  boolean isSecure();

  String getAuthenticationScheme();

}
