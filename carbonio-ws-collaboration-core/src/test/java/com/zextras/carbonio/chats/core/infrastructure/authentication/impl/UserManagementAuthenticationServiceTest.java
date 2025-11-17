// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheHandler;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserMyself;
import com.zextras.carbonio.usermanagement.enumerations.UserStatus;
import com.zextras.carbonio.usermanagement.enumerations.UserType;
import com.zextras.carbonio.usermanagement.exceptions.InternalServerError;
import com.zextras.carbonio.usermanagement.exceptions.UnAuthorized;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
        new UserManagementAuthenticationService(userManagementClient, new CacheHandler());
  }

  @Nested
  @DisplayName("Validate credentials tests")
  class getUserMyselfTests {

    @Test
    @DisplayName("Returns the authenticated user if validation was successful")
    void getUserMyself_testOk() {
      Map<String, String> attributes = new HashMap<>();
      attributes.put("carbonioFeatureWscEnabled", "TRUE");
      UserMyself mockUser = new UserMyself(
          new UserId("myUser"),
          "myUser@example.com",
          "Test User",
          "example.com",
          UserStatus.ACTIVE,
          Locale.ENGLISH,
          UserType.INTERNAL,
          attributes
      );

      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=tokenz"))
          .thenReturn(Try.success(mockUser));

      Optional<UserMyself> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isPresent());
      assertEquals("myUser", userMyself.get().getId().getUserId());
      assertEquals(UserStatus.ACTIVE, userMyself.get().getStatus());
    }

    @Test
    @DisplayName("Returns an empty optional if the token could not be verified")
    void getUserMyself_testFailingToken() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=tokenz"))
          .thenReturn(Try.failure(new UnAuthorized()));

      Optional<UserMyself> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if credential is null")
    void getUserMyself_testEmptyCredentials() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=null"))
          .thenReturn(Try.failure(new UnAuthorized()));

      Optional<UserMyself> userMyself =
          userManagementAuthenticationService.getUserMyself(null);

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not present")
    void getUserMyself_testNoZmAuthToken() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=null"))
          .thenReturn(Try.failure(new UnAuthorized()));

      Optional<UserMyself> userMyself =
          userManagementAuthenticationService.getUserMyself(null);

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the validation fails for a generic error")
    void getUserMyself_testGenericFailure() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=tokenz"))
          .thenReturn(Try.failure(new InternalServerError(new Exception())));

      Optional<UserMyself> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isEmpty());
    }
  }
}