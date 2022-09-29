package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EventsWebSocketAuthenticationFilter implements Filter {

  private final AuthenticationService authenticationService;

  public EventsWebSocketAuthenticationFilter(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  public static EventsWebSocketAuthenticationFilter create(AuthenticationService authenticationService) {
    return new EventsWebSocketAuthenticationFilter(authenticationService);
  }

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(
    ServletRequest request, ServletResponse response, FilterChain chain
  ) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    Map<AuthenticationMethod, String> credentials =
      Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[]{}))
        .filter(cookie ->
          Arrays.stream(AuthenticationMethod.values()).map(AuthenticationMethod::name).collect(Collectors.toList())
            .contains(cookie.getName()))
        .collect(Collectors.toMap(c -> AuthenticationMethod.valueOf(c.getName()), Cookie::getValue));
    if (credentials.isEmpty()) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(401);
      return;
    }
    Optional<String> userIdOpt = authenticationService.validateCredentials(credentials);
    if (userIdOpt.isEmpty()) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      httpServletResponse.setStatus(401);
      return;
    }
    httpRequest.getSession().setAttribute("userId", userIdOpt.get());
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
