// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    Map<AuthenticationMethod, String> credentials =
        Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[] {}))
            .filter(
                cookie ->
                    Arrays.stream(AuthenticationMethod.values())
                        .map(AuthenticationMethod::name)
                        .collect(Collectors.toList())
                        .contains(cookie.getName()))
            .collect(
                Collectors.toMap(c -> AuthenticationMethod.valueOf(c.getName()), Cookie::getValue));
    if (credentials.isEmpty()) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(401);
      return;
    }
    Optional<String> userId = authenticationService.validateCredentials(credentials);
    if (userId.isEmpty()) {
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
