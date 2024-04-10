// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class AuthenticationFilterTest {

  private final AuthenticationFilter authenticationFilter;
  private final AuthenticationService authenticationService;

  public AuthenticationFilterTest() {
    authenticationService = mock(AuthenticationService.class);
    authenticationFilter = new AuthenticationFilter(authenticationService);
  }

  @Nested
  @DisplayName("Filter tests")
  class FilterTests {

    @Test
    @DisplayName("Sets the correct security context")
    public void filter_testOk() {
      UUID userId = UUID.randomUUID();
      ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      Map<AuthenticationMethod, String> credentials =
          Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "token");
      when(authenticationService.validateCredentials(credentials))
          .thenReturn(Optional.of(userId.toString()));

      authenticationFilter.filter(requestContext);

      ArgumentCaptor<SecurityContextImpl> contextCaptor =
          ArgumentCaptor.forClass(SecurityContextImpl.class);
      verify(requestContext, times(1)).setSecurityContext(contextCaptor.capture());
      SecurityContextImpl capturedContext = contextCaptor.getValue();
      assertEquals(userId.toString(), ((UserPrincipal) capturedContext.getUserPrincipal()).getId());
      assertEquals(
          credentials, ((UserPrincipal) capturedContext.getUserPrincipal()).getAuthCredentials());
      assertFalse(((UserPrincipal) capturedContext.getUserPrincipal()).isSystemUser());
    }

    @Test
    @DisplayName("Creates empty user principal if no token is present")
    public void filter_testTokenNotAuthenticated() {
      ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
      when(requestContext.getCookies()).thenReturn(Map.of());

      authenticationFilter.filter(requestContext);

      ArgumentCaptor<SecurityContextImpl> contextCaptor =
          ArgumentCaptor.forClass(SecurityContextImpl.class);
      verify(requestContext, times(1)).setSecurityContext(contextCaptor.capture());
      SecurityContextImpl capturedContext = contextCaptor.getValue();
      assertNull(capturedContext.getUserPrincipal());
    }

    @Test
    @DisplayName("Throws an unauthorized exception if the token is not valid")
    public void filter_testTokenNotValid() {
      ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      Map<AuthenticationMethod, String> credentials =
          Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "token");
      when(authenticationService.validateCredentials(credentials)).thenReturn(Optional.empty());

      assertThrows(UnauthorizedException.class, () -> authenticationFilter.filter(requestContext));
    }
  }
}
