package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.infrastructure.account.AccountService;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final String AUTHORIZATION_COOKIE = "ZM_AUTH_TOKEN";

  private final AccountService accountService;

  @Inject
  public AuthenticationFilter(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Cookie cookie = Optional.ofNullable(requestContext.getCookies().get(AUTHORIZATION_COOKIE)).orElse(null);
    requestContext.setSecurityContext(
      SecurityContextImpl.create(
        UserPrincipal.create(accountService.validateToken(cookie == null ? null : cookie.getValue()).orElse(null))
          .cookie(requestContext.getHeaderString("Cookie"))));
  }
}

