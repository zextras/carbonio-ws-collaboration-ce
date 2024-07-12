// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
  public void init(FilterConfig filterConfig) {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    Optional<String> authToken =
        Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[] {}))
            .filter(cookie -> AuthenticationMethod.ZM_AUTH_TOKEN.name().equals(cookie.getName()))
            .findAny()
            .map(Cookie::getValue);
    if (authToken.isEmpty()) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(401);
      return;
    }
    Optional<String> userId = authenticationService.validateCredentials(authToken.get());
    if (userId.isEmpty()) {
      ChatsLogger.warn("Websocket authentication failed for token " + authToken.get());
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(401);
      return;
    }
    httpRequest.getSession().setAttribute("userId", userId.get());
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
