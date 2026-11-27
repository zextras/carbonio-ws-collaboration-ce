// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.UriInfo;
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
  private final UserAccessPolicy userAccessPolicy;

  public AuthenticationFilterTest() {
    authenticationService = mock(AuthenticationService.class);
    userAccessPolicy = mock(UserAccessPolicy.class);
    when(userAccessPolicy.isAllowed(any(), any(), any())).thenReturn(true);
    authenticationFilter = new AuthenticationFilter(authenticationService, userAccessPolicy);
  }

  private ContainerRequestContext requestContextFor(String path) {
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn(path);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    return requestContext;
  }

  private MyselfDto buildMyselfDto(
      String userId,
      String email,
      String fullName,
      String domain,
      String type,
      String status,
      Map<String, String> capabilities,
      String... features) {
    UserInfoDto info =
        new UserInfoDto()
            .userId(userId)
            .email(email)
            .fullName(fullName)
            .domain(domain)
            .type(type)
            .status(status);
    MyselfDto myself = new MyselfDto().info(info).locale("en").capabilities(capabilities);
    for (String feature : features) {
      myself.addFeaturesItem(feature);
    }
    return myself;
  }

  @Nested
  @DisplayName("Filter tests")
  class FilterTests {

    @Test
    @DisplayName("Sets the correct security context for an internal user with WSC enabled")
    void filter_testOk() {
      UUID userId = UUID.randomUUID();
      Map<String, String> capabilities = Map.of("carbonioFeatureWscEnabled", "TRUE");
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "user@example.com",
              "Test User",
              "example.com",
              "INTERNAL",
              "active",
              capabilities,
              "carbonioFeatureWscEnabled");

      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      authenticationFilter.filter(requestContext);

      ArgumentCaptor<SecurityContextImpl> contextCaptor =
          ArgumentCaptor.forClass(SecurityContextImpl.class);
      verify(requestContext, times(1)).setSecurityContext(contextCaptor.capture());
      SecurityContextImpl capturedContext = contextCaptor.getValue();
      UserPrincipal userPrincipal = (UserPrincipal) capturedContext.getUserPrincipal();
      assertEquals(userId.toString(), userPrincipal.getId());
      assertTrue(userPrincipal.getAuthToken().isPresent());
      assertEquals("token", userPrincipal.getAuthToken().get());
      assertEquals(UserType.INTERNAL, userPrincipal.getUserType());
      assertEquals("user@example.com", userPrincipal.getEmail());
      assertEquals("Test User", userPrincipal.getName());
    }

    @Test
    @DisplayName("Sets the correct security context for a guest user")
    void filter_testOkForGuestUser() {
      UUID userId = UUID.randomUUID();
      Map<String, String> capabilities = Map.of("carbonioFeatureWscEnabled", "TRUE");
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "guest@example.com",
              "Guest User",
              "example.com",
              "GUEST",
              "active",
              capabilities,
              "carbonioFeatureWscEnabled");

      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      authenticationFilter.filter(requestContext);

      ArgumentCaptor<SecurityContextImpl> contextCaptor =
          ArgumentCaptor.forClass(SecurityContextImpl.class);
      verify(requestContext, times(1)).setSecurityContext(contextCaptor.capture());
      SecurityContextImpl capturedContext = contextCaptor.getValue();
      UserPrincipal userPrincipal = (UserPrincipal) capturedContext.getUserPrincipal();
      assertEquals(userId.toString(), userPrincipal.getId());
    }

    @Test
    @DisplayName("Creates empty user principal if no token is present")
    void filter_testTokenNotAuthenticated() {
      ContainerRequestContext requestContext = requestContextFor("rooms");
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
    void filter_testTokenNotValid() {
      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.empty());

      assertThrows(UnauthorizedException.class, () -> authenticationFilter.filter(requestContext));
    }

    @Test
    @DisplayName("Throws a forbidden exception if user status is not ACTIVE")
    void filter_testUserNotActive() {
      UUID userId = UUID.randomUUID();
      Map<String, String> capabilities = Map.of("carbonioFeatureWscEnabled", "TRUE");
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "user@example.com",
              "Test User",
              "example.com",
              "INTERNAL",
              "closed",
              capabilities,
              "carbonioFeatureWscEnabled");

      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      assertThrows(ForbiddenException.class, () -> authenticationFilter.filter(requestContext));
    }

    @Test
    @DisplayName("Throws a forbidden exception if WSC feature is disabled for internal user")
    void filter_testWscFeatureDisabledForInternalUser() {
      UUID userId = UUID.randomUUID();
      Map<String, String> capabilities = Map.of("carbonioFeatureWscEnabled", "FALSE");
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "user@example.com",
              "Test User",
              "example.com",
              "INTERNAL",
              "active",
              capabilities);

      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      assertThrows(ForbiddenException.class, () -> authenticationFilter.filter(requestContext));
    }

    @Test
    @DisplayName("Throws a forbidden exception if WSC feature is disabled for guest user")
    void filter_testWscFeatureDisabledForGuestUser() {
      UUID userId = UUID.randomUUID();
      Map<String, String> capabilities = Map.of("carbonioFeatureWscEnabled", "FALSE");
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "user@example.com",
              "Test User",
              "example.com",
              "GUEST",
              "active",
              capabilities);

      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      assertThrows(ForbiddenException.class, () -> authenticationFilter.filter(requestContext));
    }

    @Test
    @DisplayName(
        "Throws a forbidden exception if WSC feature attribute is missing for internal user")
    void filter_testWscFeatureMissingForInternalUser() {
      UUID userId = UUID.randomUUID();
      Map<String, String> capabilities = Map.of();
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "user@example.com",
              "Test User",
              "example.com",
              "INTERNAL",
              "active",
              capabilities);

      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      assertThrows(ForbiddenException.class, () -> authenticationFilter.filter(requestContext));
    }

    @Test
    @DisplayName("Throws a forbidden exception if the access policy denies the user")
    void filter_testAccessPolicyDenied() {
      UUID userId = UUID.randomUUID();
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "user@example.com",
              "Test User",
              "example.com",
              "INTERNAL",
              "active",
              Map.of("carbonioFeatureWscEnabled", "TRUE"),
              "carbonioFeatureWscEnabled");
      ContainerRequestContext requestContext = requestContextFor("rooms");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));
      when(userAccessPolicy.isAllowed(userId.toString(), UserType.INTERNAL, "rooms"))
          .thenReturn(false);

      assertThrows(ForbiddenException.class, () -> authenticationFilter.filter(requestContext));
      verify(requestContext, never()).setSecurityContext(any());
    }

    @Test
    @DisplayName("Passes user id, user type and request path to the access policy")
    void filter_testAccessPolicyReceivesRequestData() {
      UUID userId = UUID.randomUUID();
      MyselfDto userMyself =
          buildMyselfDto(
              userId.toString(),
              "guest@example.com",
              "Guest User",
              "example.com",
              "GUEST",
              "active",
              Map.of("carbonioFeatureWscEnabled", "TRUE"),
              "carbonioFeatureWscEnabled");
      ContainerRequestContext requestContext = requestContextFor("license");
      when(requestContext.getCookies())
          .thenReturn(Map.of("ZM_AUTH_TOKEN", new Cookie("ZM_AUTH_TOKEN", "token")));
      when(authenticationService.getUserMyself("token")).thenReturn(Optional.of(userMyself));

      authenticationFilter.filter(requestContext);

      verify(userAccessPolicy).isAllowed(userId.toString(), UserType.GUEST, "license");
    }

    @Test
    @DisplayName("Does not consult the access policy for anonymous requests")
    void filter_testAnonymousSkipsAccessPolicy() {
      ContainerRequestContext requestContext = requestContextFor("health/live");
      when(requestContext.getCookies()).thenReturn(Map.of());

      authenticationFilter.filter(requestContext);

      verify(userAccessPolicy, never()).isAllowed(any(), any(), any());
    }
  }
}
