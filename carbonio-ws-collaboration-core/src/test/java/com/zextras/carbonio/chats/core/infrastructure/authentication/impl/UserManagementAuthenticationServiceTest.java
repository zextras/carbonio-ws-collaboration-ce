// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementAuthenticationServiceTest {

  private static final String USER_MANAGEMENT_BASE_URL = "http://127.78.0.2:20001";

  private UserManagementAuthenticationService userManagementAuthenticationService;
  private UserResourceApi userResourceApi;

  public UserManagementAuthenticationServiceTest() {
    this.userResourceApi = mock(UserResourceApi.class);
    HttpClient httpClient = mock(HttpClient.class);
    this.userManagementAuthenticationService =
        new UserManagementAuthenticationService(
            userResourceApi, httpClient, USER_MANAGEMENT_BASE_URL);
  }

  private MyselfDto buildMyselfDto(
      String userId,
      String email,
      String fullName,
      String domain,
      String type,
      String status,
      String... features) {
    UserInfoDto info =
        new UserInfoDto()
            .userId(userId)
            .email(email)
            .fullName(fullName)
            .domain(domain)
            .type(type)
            .status(status);
    MyselfDto myself = new MyselfDto().info(info).locale("en");
    for (String feature : features) {
      myself.addFeaturesItem(feature);
    }
    return myself;
  }

  @Nested
  @DisplayName("Validate credentials tests")
  class getUserMyselfTests {

    @Test
    @DisplayName("Returns the authenticated user if validation was successful")
    void getUserMyself_testOk() throws ApiException {
      MyselfDto mockUser =
          buildMyselfDto(
              "myUser",
              "myUser@example.com",
              "Test User",
              "example.com",
              "INTERNAL",
              "active",
              "carbonioFeatureWscEnabled");

      when(userResourceApi.internalUsersMyselfGet(anyMap())).thenReturn(mockUser);

      Optional<MyselfDto> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isPresent());
      assertEquals("myUser", userMyself.get().getInfo().getUserId());
      assertEquals("active", userMyself.get().getInfo().getStatus());
    }

    @Test
    @DisplayName("Returns an empty optional if the token could not be verified")
    void getUserMyself_testFailingToken() throws ApiException {
      when(userResourceApi.internalUsersMyselfGet(anyMap()))
          .thenThrow(new ApiException(401, "Unauthorized"));

      Optional<MyselfDto> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if credential is null")
    void getUserMyself_testEmptyCredentials() {
      Optional<MyselfDto> userMyself =
          userManagementAuthenticationService.getUserMyself(null);

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not present")
    void getUserMyself_testNoZmAuthToken() {
      Optional<MyselfDto> userMyself =
          userManagementAuthenticationService.getUserMyself(null);

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the validation fails for a generic error")
    void getUserMyself_testGenericFailure() throws ApiException {
      when(userResourceApi.internalUsersMyselfGet(anyMap()))
          .thenThrow(new ApiException(500, "Internal Server Error"));

      Optional<MyselfDto> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isEmpty());
    }
  }

  @Nested
  @DisplayName("Get User profile tests")
  class GetUserProfileTests {

    @Test
    @DisplayName("Returns the authenticated user's info if user management successfully returns it")
    void getUserProfile_testOk() throws ApiException {
      MyselfDto mockUser =
          buildMyselfDto(
              "my-user-id",
              "myuser@example.com",
              "My User",
              "example.com",
              "INTERNAL",
              "active",
              "carbonioFeatureWscEnabled");

      when(userResourceApi.internalUsersMyselfGet(anyMap())).thenReturn(mockUser);

      Optional<MyselfDto> userProfile =
          userManagementAuthenticationService.getUserMyself("tokenz");

      verify(userResourceApi, times(1)).internalUsersMyselfGet(anyMap());

      assertTrue(userProfile.isPresent());
      assertEquals("my-user-id", userProfile.get().getInfo().getUserId());
      assertEquals("myuser@example.com", userProfile.get().getInfo().getEmail());
      assertEquals("My User", userProfile.get().getInfo().getFullName());
      assertEquals("example.com", userProfile.get().getInfo().getDomain());
      assertEquals("INTERNAL", userProfile.get().getInfo().getType());
    }

    @Test
    @DisplayName("Calls UM service on every request (no local cache)")
    void getUserProfile_testNoLocalCache() throws ApiException {
      MyselfDto mockUser =
          buildMyselfDto(
              "my-user-id",
              "myuser@example.com",
              "My User",
              "example.com",
              "INTERNAL",
              "active",
              "carbonioFeatureWscEnabled");

      when(userResourceApi.internalUsersMyselfGet(anyMap())).thenReturn(mockUser);

      userManagementAuthenticationService.getUserMyself("tokenz");
      Optional<MyselfDto> userProfile =
          userManagementAuthenticationService.getUserMyself("tokenz");

      verify(userResourceApi, times(2)).internalUsersMyselfGet(anyMap());

      assertTrue(userProfile.isPresent());
      assertEquals("my-user-id", userProfile.get().getInfo().getUserId());
      assertEquals("myuser@example.com", userProfile.get().getInfo().getEmail());
      assertEquals("My User", userProfile.get().getInfo().getFullName());
      assertEquals("example.com", userProfile.get().getInfo().getDomain());
      assertEquals("INTERNAL", userProfile.get().getInfo().getType());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not authenticated")
    void getUserProfile_testTokenNotAuthenticated() throws ApiException {
      when(userResourceApi.internalUsersMyselfGet(anyMap()))
          .thenThrow(new ApiException(404, "Not Found"));

      Optional<MyselfDto> userProfile =
          userManagementAuthenticationService.getUserMyself("tokenz");

      verify(userResourceApi, times(1)).internalUsersMyselfGet(anyMap());

      assertTrue(userProfile.isEmpty());
    }
  }
}
