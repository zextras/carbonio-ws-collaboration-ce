// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EventsWebSocketAuthenticationFilter implements Filter {

  private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

  private final AuthenticationService authenticationService;
  private final List<String> supportedSubprotocols;

  public EventsWebSocketAuthenticationFilter(
      AuthenticationService authenticationService, List<String> supportedSubprotocols) {
    this.authenticationService = authenticationService;
    this.supportedSubprotocols = supportedSubprotocols;
  }

  public static EventsWebSocketAuthenticationFilter create(
      AuthenticationService authenticationService) {
    return create(authenticationService, VersionProvider.getSupportedVersions());
  }

  public static EventsWebSocketAuthenticationFilter create(
      AuthenticationService authenticationService, List<String> supportedSubprotocols) {
    return new EventsWebSocketAuthenticationFilter(authenticationService, supportedSubprotocols);
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

    Optional<MyselfDto> userMyselfOpt = authenticationService.getUserMyself(authToken.get());

    if (userMyselfOpt.isEmpty()) {
      ChatsLogger.warn("Websocket authentication failed for token " + authToken.get());
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }

    MyselfDto userMyself = userMyselfOpt.get();

    // Check if user status is ACTIVE
    if (!userMyself.getInfo().getStatus().equalsIgnoreCase("active")) {
      ChatsLogger.warn("Websocket authentication failed: user is not active");
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }

    // Check if carbonioFeatureWscEnabled is in features list
    boolean wscEnabled =
        userMyself.getFeatures().contains(CarbonioAttribute.FEATURE_WSC_ENABLED.getValue());
    if (!wscEnabled) {
      ChatsLogger.warn("Websocket authentication failed: WSC feature not enabled for user");
      httpServletResponse.setStatus(Status.UNAUTHORIZED.getStatusCode());
      return;
    }
    Map<String, String> capabilities = userMyself.getCapabilities();

    List<String> requestedSubprotocols = requestedSubprotocols(httpRequest);
    if (!requestedSubprotocols.isEmpty()
        && EventsWebSocketEndpointConfigurator.selectSubprotocol(
                supportedSubprotocols, requestedSubprotocols)
            .isEmpty()) {
      ChatsLogger.warn(
          "Websocket handshake rejected: no supported sub-protocol in " + requestedSubprotocols);
      httpServletResponse.setStatus(Status.BAD_REQUEST.getStatusCode());
      return;
    }

    String userId = userMyself.getInfo().getUserId();
    httpRequest.getSession().setAttribute("userId", userId);
    chain.doFilter(request, response);
  }

  private static List<String> requestedSubprotocols(HttpServletRequest httpRequest) {
    return Collections.list(httpRequest.getHeaders(SEC_WEBSOCKET_PROTOCOL)).stream()
        .flatMap(header -> Arrays.stream(header.split(",")))
        .map(String::trim)
        .filter(subprotocol -> !subprotocol.isEmpty())
        .toList();
  }
}
