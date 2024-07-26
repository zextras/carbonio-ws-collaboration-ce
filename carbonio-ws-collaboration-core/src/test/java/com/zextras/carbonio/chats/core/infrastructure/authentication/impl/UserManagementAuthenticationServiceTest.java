// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheHandler;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserMyself;
import com.zextras.carbonio.usermanagement.exceptions.InternalServerError;
import com.zextras.carbonio.usermanagement.exceptions.UnAuthorized;
import com.zextras.carbonio.usermanagement.exceptions.UserNotFound;
import io.vavr.control.Try;
import java.util.Locale;
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
  @DisplayName("Validate token tests")
  class ValidateTokenTests {

    @Test
    @DisplayName("Returns the authenticated user's id if user management successfully returns it")
    void validateToken_testOk() {
      when(userManagementClient.validateUserToken("tokenz"))
          .thenReturn(Try.success(new UserId("my-user-id")));

      Optional<String> userId = userManagementAuthenticationService.validateCredentials("tokenz");

      assertTrue(userId.isPresent());
      assertEquals("my-user-id", userId.get());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not authenticated")
    void validateToken_testTokenNotAuthenticated() {
      when(userManagementClient.validateUserToken("tokenz"))
          .thenReturn(Try.failure(new UnAuthorized()));

      Optional<String> userId = userManagementAuthenticationService.validateCredentials("tokenz");

      assertTrue(userId.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the token passed is null")
    void validateToken_testNullAuthToken() {
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

  @Nested
  @DisplayName("Get User profile tests")
  class GetUserProfileTests {

    @Test
    @DisplayName("Returns the authenticated user's info if user management successfully returns it")
    void getUserProfile_testOk() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=tokenz"))
          .thenReturn(
              Try.success(
                  new UserMyself(
                      new UserId("my-user-id"),
                      "myuser@example.com",
                      "My User",
                      "example.com",
                      Locale.getDefault(),
                      com.zextras.carbonio.usermanagement.enumerations.UserType.INTERNAL)));

      Optional<UserProfile> userProfile =
          userManagementAuthenticationService.getUserProfile("tokenz");

      verify(userManagementClient, times(1)).getUserMyself("ZM_AUTH_TOKEN=tokenz");

      assertTrue(userProfile.isPresent());
      assertEquals("my-user-id", userProfile.get().getId());
      assertEquals("myuser@example.com", userProfile.get().getEmail());
      assertEquals("My User", userProfile.get().getName());
      assertEquals("example.com", userProfile.get().getDomain());
      assertEquals(UserType.INTERNAL, userProfile.get().getType());
    }

    @Test
    @DisplayName("Returns the authenticated user's info already cached")
    void getUserProfile_testOkAlreadyCached() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=tokenz"))
          .thenReturn(
              Try.success(
                  new UserMyself(
                      new UserId("my-user-id"),
                      "myuser@example.com",
                      "My User",
                      "example.com",
                      Locale.getDefault(),
                      com.zextras.carbonio.usermanagement.enumerations.UserType.INTERNAL)));

      userManagementAuthenticationService.getUserProfile("tokenz");
      Optional<UserProfile> userProfile =
          userManagementAuthenticationService.getUserProfile("tokenz");

      verify(userManagementClient, times(1)).getUserMyself("ZM_AUTH_TOKEN=tokenz");

      assertTrue(userProfile.isPresent());
      assertEquals("my-user-id", userProfile.get().getId());
      assertEquals("myuser@example.com", userProfile.get().getEmail());
      assertEquals("My User", userProfile.get().getName());
      assertEquals("example.com", userProfile.get().getDomain());
      assertEquals(UserType.INTERNAL, userProfile.get().getType());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not authenticated")
    void getUserProfile_testTokenNotAuthenticated() {
      when(userManagementClient.getUserMyself("ZM_AUTH_TOKEN=tokenz"))
          .thenReturn(Try.failure(new UserNotFound("User tokenz not found")));

      Optional<UserProfile> userProfile =
          userManagementAuthenticationService.getUserProfile("tokenz");

      verify(userManagementClient, times(1)).getUserMyself("ZM_AUTH_TOKEN=tokenz");

      assertTrue(userProfile.isEmpty());
    }
  }
}
