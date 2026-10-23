// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class EventsWebSocketAuthenticationFilterTest {

  private final AuthenticationService authenticationService = mock(AuthenticationService.class);
  private final EventsWebSocketAuthenticationFilter filter =
      EventsWebSocketAuthenticationFilter.create(authenticationService);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  private void authenticatedRequestOffering(List<String> subprotocols) {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("ZM_AUTH_TOKEN", "token")});
    MyselfDto myself =
        new MyselfDto()
            .info(new UserInfoDto().userId(UUID.randomUUID().toString()).status("active"))
            .capabilities(Map.of());
    myself.addFeaturesItem(CarbonioAttribute.FEATURE_WSC_ENABLED.getValue());
    when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(myself));
    when(request.getHeaders("Sec-WebSocket-Protocol"))
        .thenReturn(Collections.enumeration(subprotocols));
    when(request.getSession()).thenReturn(mock(HttpSession.class));
  }

  @Test
  @DisplayName("Rejects with 400 a handshake that offers only unsupported sub-protocols")
  void unsupportedSubprotocols_badRequest() throws Exception {
    authenticatedRequestOffering(List.of("1.8.0, 0.0.1"));

    filter.doFilter(request, response, chain);

    verify(response, times(1)).setStatus(400);
    verify(chain, never()).doFilter(request, response);
  }

  @Test
  @DisplayName("Lets through a handshake that offers a supported sub-protocol")
  void supportedSubprotocol_continues() throws Exception {
    authenticatedRequestOffering(List.of("1.8.0", VersionProvider.getVersion()));

    filter.doFilter(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  @DisplayName("Lets through a handshake without sub-protocols")
  void noSubprotocol_continues() throws Exception {
    authenticatedRequestOffering(List.of());

    filter.doFilter(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }
}
