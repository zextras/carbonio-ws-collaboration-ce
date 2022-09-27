package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Request;

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
    Map<AuthenticationMethod, String> credentials = Collections.list(((Request) request).getHeaderNames()).stream()
      .filter(header ->
        Arrays.stream(AuthenticationMethod.values()).map(AuthenticationMethod::name)
          .collect(Collectors.toList()).contains(header))
      .collect(Collectors.toMap(AuthenticationMethod::valueOf, ((Request) request)::getHeader));
    if (credentials.isEmpty()) {
      throw new UnauthorizedException();
    } else {
      httpRequest.getSession().setAttribute("userId",
        authenticationService.validateCredentials(credentials)
          .orElseThrow(UnauthorizedException::new));
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
