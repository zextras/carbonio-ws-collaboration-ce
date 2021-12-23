package com.zextras.chats.core.web.security;

import java.security.Principal;
import java.util.Optional;

public interface MockSecurityContext {

  Optional<Principal> getUserPrincipal();

  boolean isUserInRole(String var1);

  boolean isSecure();

  String getAuthenticationScheme();

  String getUserPrincipalId();

}
