package com.zextras.chats.core.web.security;

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
