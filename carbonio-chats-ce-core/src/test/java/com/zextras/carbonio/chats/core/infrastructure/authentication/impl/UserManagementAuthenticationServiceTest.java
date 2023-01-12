// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.exceptions.InternalServerError;
import com.zextras.carbonio.usermanagement.exceptions.Unauthorized;
import io.vavr.control.Try;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementAuthenticationServiceTest {

  private UserManagementAuthenticationService userManagementAuthenticationService;
  private UserManagementClient                userManagementClient;

  public UserManagementAuthenticationServiceTest() {
    this.userManagementClient = mock(UserManagementClient.class);
    this.userManagementAuthenticationService = new UserManagementAuthenticationService(userManagementClient);
  }

  @Nested
  @DisplayName("Validate token tests")
  class ValidateTokenTests {

    @Test
    @DisplayName("Returns the authenticated user's id if it was successful")
    public void validateToken_testOk() {
      when(userManagementClient.validateUserToken("tokenz"))
        .thenReturn(Try.success(new UserId("myUser")));

      Map<AuthenticationMethod, String> credentials = Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "tokenz");
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(credentials);

      assertTrue(userId.isPresent());
      assertEquals("myUser", userId.get());
    }

    @Test
    @DisplayName("Returns an empty optional if the token could not be verified")
    public void validateToken_testFailingToken() {
      when(userManagementClient.validateUserToken("tokenz"))
        .thenReturn(Try.failure(new Unauthorized()));

      Map<AuthenticationMethod, String> credentials = Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "tokenz");
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(credentials);

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if credential map is null")
    public void validateToken_testEmptyCredentials() {
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(null);

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not in the credentials")
    public void validateToken_testNoZmAuthToken() {
      Map<AuthenticationMethod, String> credentials = Map.of();
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(credentials);

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the validation fails for a generic error")
    public void validateToken_testGenericFailure() {
      when(userManagementClient.validateUserToken("tokenz"))
        .thenReturn(Try.failure(new InternalServerError(new Exception())));

      Map<AuthenticationMethod, String> credentials = Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "tokenz");
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(credentials);

      assertTrue(userId.isEmpty());
    }

  }

}