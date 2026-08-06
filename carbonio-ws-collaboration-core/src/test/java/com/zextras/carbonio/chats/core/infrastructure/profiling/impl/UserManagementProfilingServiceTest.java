// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementProfilingServiceTest {

  private static final String USER_MANAGEMENT_BASE_URL = "http://127.78.0.2:20001";

  private final UserManagementProfilingService profilingService;
  private final UserResourceApi userResourceApi;

  public UserManagementProfilingServiceTest() {
    this.userResourceApi = mock(UserResourceApi.class);
    HttpClient httpClient = mock(HttpClient.class);
    this.profilingService =
        new UserManagementProfilingService(userResourceApi, httpClient, USER_MANAGEMENT_BASE_URL);
  }

  @AfterEach
  public void cleanup() {
    reset(userResourceApi);
  }

  private UserInfoDto buildUserInfoDto(
      String userId, String email, String fullName, String domain, String status, String type) {
    return new UserInfoDto()
        .userId(userId)
        .email(email)
        .fullName(fullName)
        .domain(domain)
        .status(status)
        .type(type);
  }

  @Nested
  @DisplayName("Get by id tests")
  class GetByIdTests {

    @Test
    @DisplayName("Returns the requested user correctly mapped")
    void getById_testOk() throws ApiException {
      UUID randomUUID = UUID.randomUUID();
      UserInfoDto userInfo =
          buildUserInfoDto(
              randomUUID.toString(),
              "email@test.com",
              "name hello",
              "mydomain.com",
              "active",
              "INTERNAL");
      when(userResourceApi.internalUsersIdUserIdGet(anyString())).thenReturn(userInfo);

      Optional<UserProfile> userProfile =
          profilingService.getById(
              UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID);

      assertTrue(userProfile.isPresent());
      assertEquals(randomUUID.toString(), userProfile.get().getId());
      assertEquals("email@test.com", userProfile.get().getEmail());
      assertEquals("name hello", userProfile.get().getName());
      assertEquals("mydomain.com", userProfile.get().getDomain());
      assertEquals(UserType.INTERNAL, userProfile.get().getType());
    }

    @Test
    @DisplayName("Returns an empty optional if the user was not found")
    void getById_testNotFound() throws ApiException {
      UUID randomUUID = UUID.randomUUID();
      when(userResourceApi.internalUsersIdUserIdGet(anyString()))
          .thenThrow(new ApiException(404, "Not Found"));

      Optional<UserProfile> userProfile =
          profilingService.getById(
              UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID);

      assertTrue(userProfile.isEmpty());
    }

    @Test
    @DisplayName("Throws a forbidden exception when the user doesn't have an authentication token")
    void getById_testForbiddenException() {
      UUID randomUUID = UUID.randomUUID();
      assertThrows(
          ForbiddenException.class,
          () ->
              profilingService.getById(
                  UserPrincipal.create(randomUUID).authToken(null), randomUUID));
    }

    @Test
    @DisplayName("Throws an exception when the call fails for any other reason")
    void getById_testException() throws ApiException {
      UUID randomUUID = UUID.randomUUID();
      when(userResourceApi.internalUsersIdUserIdGet(anyString()))
          .thenThrow(new ApiException(500, "Internal Server Error"));
      assertThrows(
          ProfilingException.class,
          () ->
              profilingService.getById(
                  UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID));
    }
  }

  @Nested
  @DisplayName("Is alive tests")
  class IsAliveTests {

    @Test
    @DisplayName(
        "Reports the dependency as unhealthy, without throwing, when User Management is"
            + " genuinely unreachable (connection refused)")
    void isAlive_testUnreachable() throws IOException {
      int closedPort;
      try (ServerSocket socket = new ServerSocket(0)) {
        closedPort = socket.getLocalPort();
      }
      UserManagementProfilingService serviceWithRealClient =
          new UserManagementProfilingService(
              userResourceApi, new HttpClient(), "http://127.0.0.1:" + closedPort);

      boolean isAlive = assertDoesNotThrow(serviceWithRealClient::isAlive);

      assertFalse(isAlive);
    }
  }
}
