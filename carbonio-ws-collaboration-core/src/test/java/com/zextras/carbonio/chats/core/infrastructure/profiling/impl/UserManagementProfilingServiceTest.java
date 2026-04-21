// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserByIdRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementProfilingServiceTest {

  private final UserManagementProfilingService profilingService;
  private final UserManagementServiceBlockingStub userManagementStub;
  private final ManagedChannel userManagementChannel;

  public UserManagementProfilingServiceTest() {
    this.userManagementStub = mock(UserManagementServiceBlockingStub.class);
    this.userManagementChannel = mock(ManagedChannel.class);
    this.profilingService =
        new UserManagementProfilingService(userManagementStub, userManagementChannel);
  }

  @AfterEach
  public void cleanup() {
    reset(userManagementStub);
  }

  private UserInfoProto buildUserInfoProto(
      String userId, String email, String fullName, String domain,
      String status, UserTypeProto type) {
    return UserInfoProto.newBuilder()
        .setUserId(userId)
        .setEmail(email)
        .setFullName(fullName)
        .setDomain(domain)
        .setStatus(status)
        .setType(type)
        .build();
  }

  @Nested
  @DisplayName("Get by id tests")
  class GetByIdTests {

    @Test
    @DisplayName("Returns the requested user correctly mapped")
    void getById_testOk() {
      UUID randomUUID = UUID.randomUUID();
      UserInfoProto userInfo = buildUserInfoProto(
          randomUUID.toString(), "email@test.com", "name hello", "mydomain.com",
          "active", UserTypeProto.INTERNAL);
      when(userManagementStub.getUserById(any(GetUserByIdRequest.class)))
          .thenReturn(UserInfoResponse.newBuilder().setUser(userInfo).build());

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
    void getById_testNotFound() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementStub.getUserById(any(GetUserByIdRequest.class)))
          .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

      Optional<UserProfile> userProfile =
          profilingService.getById(
              UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID);

      assertTrue(userProfile.isEmpty());
    }

    @Test
    @DisplayName("Throws an exception when the call fails for any other reason")
    void getById_testException() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementStub.getUserById(any(GetUserByIdRequest.class)))
          .thenThrow(new StatusRuntimeException(Status.INTERNAL));
      assertThrows(
          ProfilingException.class,
          () ->
              profilingService.getById(
                  UserPrincipal.create(randomUUID).authToken("cookie"), randomUUID));
    }
  }
}
