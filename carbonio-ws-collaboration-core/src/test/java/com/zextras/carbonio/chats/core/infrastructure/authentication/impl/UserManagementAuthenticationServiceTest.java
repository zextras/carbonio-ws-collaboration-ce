// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.exceptions.InternalServerError;
import com.zextras.carbonio.usermanagement.exceptions.UnAuthorized;
import io.vavr.control.Try;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementAuthenticationServiceTest {

  private UserManagementAuthenticationService userManagementAuthenticationService;
  private UserManagementClient userManagementClient;

  public UserManagementAuthenticationServiceTest() {
    this.userManagementClient = mock(UserManagementClient.class);
    this.userManagementAuthenticationService =
        new UserManagementAuthenticationService(userManagementClient);
  }

  @Nested
  @DisplayName("Validate token tests")
  class ValidateTokenTests {

    @Test
    @DisplayName("Returns the authenticated user's id if it was successful")
    void validateToken_testOk() {
      when(userManagementClient.validateUserToken("tokenz"))
          .thenReturn(Try.success(new UserId("myUser")));

      Optional<String> userId = userManagementAuthenticationService.validateCredentials("tokenz");

      assertTrue(userId.isPresent());
      assertEquals("myUser", userId.get());
    }

    @Test
    @DisplayName("Returns an empty optional if the token could not be verified")
    void validateToken_testFailingToken() {
      when(userManagementClient.validateUserToken("tokenz"))
          .thenReturn(Try.failure(new UnAuthorized()));

      Optional<String> userId = userManagementAuthenticationService.validateCredentials("tokenz");

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if credential map is null")
    void validateToken_testEmptyCredentials() {
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(null);

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not in the credentials")
    void validateToken_testNoZmAuthToken() {
      Optional<String> userId = userManagementAuthenticationService.validateCredentials(null);

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the validation fails for a generic error")
    void validateToken_testGenericFailure() {
      when(userManagementClient.validateUserToken("tokenz"))
          .thenReturn(Try.failure(new InternalServerError(new Exception())));

      Optional<String> userId = userManagementAuthenticationService.validateCredentials("tokenz");

      assertTrue(userId.isEmpty());
    }
  }
}
