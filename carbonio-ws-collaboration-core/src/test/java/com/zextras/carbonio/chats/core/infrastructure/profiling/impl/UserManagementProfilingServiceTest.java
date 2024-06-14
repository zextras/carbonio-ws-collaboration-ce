// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import com.zextras.carbonio.usermanagement.enumerations.UserStatus;
import com.zextras.carbonio.usermanagement.enumerations.UserType;
import com.zextras.carbonio.usermanagement.exceptions.UserNotFound;
import io.vavr.control.Try;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementProfilingServiceTest {

  private final UserManagementProfilingService profilingService;
  private final UserManagementClient userManagementClient;

  public UserManagementProfilingServiceTest() {
    this.userManagementClient = mock(UserManagementClient.class);
    this.profilingService = new UserManagementProfilingService(userManagementClient);
  }

  @AfterEach
  public void cleanup() {
    reset(userManagementClient);
  }

  @Nested
  @DisplayName("Get by id tests")
  class GetByIdTests {

    @Test
    @DisplayName("Returns the requested user correctly mapped")
    void getById_testOk() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementClient.getUserById("ZM_AUTH_TOKEN=cookie", randomUUID.toString()))
          .thenReturn(
              Try.success(
                  new UserInfo(
                      new UserId(randomUUID.toString()),
                      "email@test.com",
                      "name hello",
                      "mydomain.com",
                      UserStatus.ACTIVE,
                      UserType.INTERNAL)));
      Optional<UserProfile> userProfile =
          profilingService.getById(
              UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID);

      assertTrue(userProfile.isPresent());
      assertEquals(randomUUID.toString(), userProfile.get().getId());
      assertEquals("email@test.com", userProfile.get().getEmail());
      assertEquals("name hello", userProfile.get().getName());
      assertEquals("mydomain.com", userProfile.get().getDomain());
    }

    @Test
    @DisplayName("Returns an empty optional if the user was not found")
    void getById_testNotFound() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementClient.getUserById("ZM_AUTH_TOKEN=cookie", randomUUID.toString()))
          .thenReturn(Try.failure(new UserNotFound(randomUUID.toString())));
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
    void getById_testException() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementClient.getUserById("ZM_AUTH_TOKEN=cookie", randomUUID.toString()))
          .thenReturn(Try.failure(new Exception()));
      assertThrows(
          ProfilingException.class,
          () ->
              profilingService.getById(
                  UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID));
    }
  }
}
