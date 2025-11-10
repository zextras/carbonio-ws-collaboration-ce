// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.usermanagement.entities.UserMyself;
import com.zextras.carbonio.usermanagement.enumerations.UserStatus;
import com.zextras.carbonio.usermanagement.enumerations.UserType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class EventsWebSocketAuthenticationFilter implements Filter {

  private final AuthenticationService authenticationService;

  public EventsWebSocketAuthenticationFilter(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  public static EventsWebSocketAuthenticationFilter create(
      AuthenticationService authenticationService) {
    return new EventsWebSocketAuthenticationFilter(authenticationService);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;

    Optional<String> authToken =
        Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[] {}))
            .filter(cookie -> AuthenticationMethod.ZM_AUTH_TOKEN.name().equals(cookie.getName()))
            .findAny()
            .map(Cookie::getValue);

    if (authToken.isEmpty()) {
      ChatsLogger.warn("Websocket authentication failed with an empty token");
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }

    Optional<UserMyself> userMyselfOpt = authenticationService.getUserMyself(authToken.get());

    if (userMyselfOpt.isEmpty()) {
      ChatsLogger.warn("Websocket authentication failed for token " + authToken.get());
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }

    UserMyself userMyself = userMyselfOpt.get();

    // Check if user status is ACTIVE
    if (!userMyself.getStatus().equals(UserStatus.ACTIVE)) {
      ChatsLogger.warn("Websocket authentication failed: user is not active");
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }

    // Check if carbonioFeatureWscEnabled is TRUE
    // TODO we only do this check for internal users, since guests have all carbonioFeatureFlags set to false;
    // this can and should probably change in the future
    String wscEnabled = userMyself.getCarbonioAttributes()
        .getOrDefault("carbonioFeatureWscEnabled", "FALSE");
    if (wscEnabled.equals("FALSE") && userMyself.getType().equals(UserType.INTERNAL)) {
      ChatsLogger.warn("Websocket authentication failed: WSC feature not enabled for user");
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }

    String userId = userMyself.getId().getUserId();
    httpRequest.getSession().setAttribute("userId", userId);
    chain.doFilter(request, response);
  }
}
