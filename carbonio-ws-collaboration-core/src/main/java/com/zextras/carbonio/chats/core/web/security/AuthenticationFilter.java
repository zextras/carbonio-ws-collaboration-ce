// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final String AUTHORIZATION_COOKIE = "ZM_AUTH_TOKEN";

  private final AuthenticationService authenticationService;

  @Inject
  public AuthenticationFilter(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    UUID queueId;
    try{
      queueId = Optional.ofNullable(requestContext.getHeaderString("queue-id"))
        .map(UUID::fromString).orElse(null);
    } catch(IllegalArgumentException e){
      throw new UnauthorizedException("Invalid queue id");
    }
    Map<AuthenticationMethod, String> credentials = new HashMap<>();
    Optional.ofNullable(requestContext.getCookies().get(AUTHORIZATION_COOKIE))
      .ifPresent(cookie -> credentials.put(AuthenticationMethod.ZM_AUTH_TOKEN, cookie.getValue()));
    if (credentials.isEmpty()) {
      //The user didn't specify any authorization, we're logging him/her as anonymous (useful for healthchecks)
      requestContext.setSecurityContext(
        SecurityContextImpl.create(
          UserPrincipal
            .create((UUID) null)
            .authCredentials(credentials)
        )
      );
    } else {
      //If the user token is invalid, we won't authenticate him/her as anonymous
      requestContext.setSecurityContext(
        SecurityContextImpl.create(
          UserPrincipal
            .create(
              authenticationService.validateCredentials(credentials).map(UUID::fromString)
                .orElseThrow(UnauthorizedException::new))
            .authCredentials(credentials)
            .queueId(queueId)
        )
      );
    }
  }
}

