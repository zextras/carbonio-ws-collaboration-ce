// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class EventsWebSocketAuthenticationFilterTest {

  private final AuthenticationService authenticationService = mock(AuthenticationService.class);
  private final UserAccessPolicy userAccessPolicy = mock(UserAccessPolicy.class);
  private final EventsWebSocketAuthenticationFilter filter =
      EventsWebSocketAuthenticationFilter.create(authenticationService, userAccessPolicy);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final HttpSession session = mock(HttpSession.class);
  private final String userId = UUID.randomUUID().toString();

  private void givenUser(String type) {
    MyselfDto myself =
        new MyselfDto()
            .info(new UserInfoDto().userId(userId).type(type).status("active"))
            .addFeaturesItem("carbonioFeatureWscEnabled");
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("ZM_AUTH_TOKEN", "token")});
    when(request.getSession()).thenReturn(session);
    when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(myself));
  }

  @Test
  void allowedUserContinuesTheChain() throws Exception {
    givenUser("INTERNAL");
    when(userAccessPolicy.isAllowed(userId, UserType.INTERNAL, "/events")).thenReturn(true);

    filter.doFilter(request, response, chain);

    verify(session).setAttribute("userId", userId);
    verify(chain).doFilter(request, response);
  }

  @Test
  void deniedUserIsRefusedWithUnauthorized() throws Exception {
    givenUser("INTERNAL");
    when(userAccessPolicy.isAllowed(userId, UserType.INTERNAL, "/events")).thenReturn(false);

    filter.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void guestTypeIsPassedToThePolicy() throws Exception {
    givenUser("GUEST");
    when(userAccessPolicy.isAllowed(userId, UserType.GUEST, "/events")).thenReturn(true);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void missingTokenIsRefusedWithoutConsultingThePolicy() throws Exception {
    when(request.getCookies()).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(userAccessPolicy, never()).isAllowed(any(), any(), any());
  }
}
