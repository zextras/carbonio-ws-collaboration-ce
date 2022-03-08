package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
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

  private final AuthenticationService authenticationService;

  @Inject
  public AuthenticationFilter(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Optional<Cookie> cookie = Optional.ofNullable(requestContext.getCookies().get(AUTHORIZATION_COOKIE));
    requestContext.setSecurityContext(
      SecurityContextImpl.create(
        UserPrincipal.create(authenticationService.validateToken(cookie.map(Cookie::getValue).orElse(null)).orElse(null))
          .cookieString(requestContext.getHeaderString("Cookie"))
      )
    );
  }
}

