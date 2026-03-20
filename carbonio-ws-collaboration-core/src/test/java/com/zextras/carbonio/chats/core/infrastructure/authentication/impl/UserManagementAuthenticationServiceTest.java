// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementAuthenticationServiceTest {

  private UserManagementAuthenticationService userManagementAuthenticationService;
  private UserManagementServiceBlockingStub userManagementStub;
  private ManagedChannel userManagementChannel;

  public UserManagementAuthenticationServiceTest() {
    this.userManagementStub = mock(UserManagementServiceBlockingStub.class);
    this.userManagementChannel = mock(ManagedChannel.class);
    this.userManagementAuthenticationService =
        new UserManagementAuthenticationService(
            userManagementStub, userManagementChannel);
  }

  private UserMyselfProto buildUserMyselfProto(
      String userId, String email, String fullName, String domain,
      UserTypeProto type, String status, String... features) {
    UserInfoProto info = UserInfoProto.newBuilder()
        .setUserId(userId)
        .setEmail(email)
        .setFullName(fullName)
        .setDomain(domain)
        .setType(type)
        .setStatus(status)
        .build();
    UserMyselfProto.Builder builder = UserMyselfProto.newBuilder()
        .setInfo(info)
        .setLocale("en");
    for (String feature : features) {
      builder.addFeatures(feature);
    }
    return builder.build();
  }

  private UserMyselfResponse buildUserMyselfResponse(UserMyselfProto userMyself) {
    return UserMyselfResponse.newBuilder().setUser(userMyself).build();
  }

  @Nested
  @DisplayName("Validate credentials tests")
  class getUserMyselfTests {

    @Test
    @DisplayName("Returns the authenticated user if validation was successful")
    void getUserMyself_testOk() {
      UserMyselfProto mockUser = buildUserMyselfProto(
          "myUser", "myUser@example.com", "Test User", "example.com",
          UserTypeProto.INTERNAL, "active", "carbonioFeatureWscEnabled");

      when(userManagementStub.getUserMyself(any(GetUserMyselfRequest.class)))
          .thenReturn(buildUserMyselfResponse(mockUser));

      Optional<UserMyselfProto> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isPresent());
      assertEquals("myUser", userMyself.get().getInfo().getUserId());
      assertEquals("active", userMyself.get().getInfo().getStatus());
    }

    @Test
    @DisplayName("Returns an empty optional if the token could not be verified")
    void getUserMyself_testFailingToken() {
      when(userManagementStub.getUserMyself(any(GetUserMyselfRequest.class)))
          .thenThrow(new StatusRuntimeException(Status.UNAUTHENTICATED));

      Optional<UserMyselfProto> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if credential is null")
    void getUserMyself_testEmptyCredentials() {
      Optional<UserMyselfProto> userMyself =
          userManagementAuthenticationService.getUserMyself(null);

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not present")
    void getUserMyself_testNoZmAuthToken() {
      Optional<UserMyselfProto> userMyself =
          userManagementAuthenticationService.getUserMyself(null);

      assertTrue(userMyself.isEmpty());
    }

    @Test
    @DisplayName("Returns an empty optional if the validation fails for a generic error")
    void getUserMyself_testGenericFailure() {
      when(userManagementStub.getUserMyself(any(GetUserMyselfRequest.class)))
          .thenThrow(new StatusRuntimeException(Status.INTERNAL));

      Optional<UserMyselfProto> userMyself =
          userManagementAuthenticationService.getUserMyself("tokenz");

      assertTrue(userMyself.isEmpty());
    }
  }

  @Nested
  @DisplayName("Get User profile tests")
  class GetUserProfileTests {

    @Test
    @DisplayName("Returns the authenticated user's info if user management successfully returns it")
    void getUserProfile_testOk() {
      UserMyselfProto mockUser = buildUserMyselfProto(
          "my-user-id", "myuser@example.com", "My User", "example.com",
          UserTypeProto.INTERNAL, "active", "carbonioFeatureWscEnabled");

      when(userManagementStub.getUserMyself(any(GetUserMyselfRequest.class)))
          .thenReturn(buildUserMyselfResponse(mockUser));

      Optional<UserMyselfProto> userProfile =
          userManagementAuthenticationService.getUserMyself("tokenz");

      verify(userManagementStub, times(1)).getUserMyself(any(GetUserMyselfRequest.class));

      assertTrue(userProfile.isPresent());
      assertEquals("my-user-id", userProfile.get().getInfo().getUserId());
      assertEquals("myuser@example.com", userProfile.get().getInfo().getEmail());
      assertEquals("My User", userProfile.get().getInfo().getFullName());
      assertEquals("example.com", userProfile.get().getInfo().getDomain());
      assertEquals(UserTypeProto.INTERNAL, userProfile.get().getInfo().getType());
    }

    @Test
    @DisplayName("Calls UM service on every request (no local cache)")
    void getUserProfile_testNoLocalCache() {
      UserMyselfProto mockUser = buildUserMyselfProto(
          "my-user-id", "myuser@example.com", "My User", "example.com",
          UserTypeProto.INTERNAL, "active", "carbonioFeatureWscEnabled");

      when(userManagementStub.getUserMyself(any(GetUserMyselfRequest.class)))
          .thenReturn(buildUserMyselfResponse(mockUser));

      userManagementAuthenticationService.getUserMyself("tokenz");
      Optional<UserMyselfProto> userProfile =
          userManagementAuthenticationService.getUserMyself("tokenz");

      verify(userManagementStub, times(2)).getUserMyself(any(GetUserMyselfRequest.class));

      assertTrue(userProfile.isPresent());
      assertEquals("my-user-id", userProfile.get().getInfo().getUserId());
      assertEquals("myuser@example.com", userProfile.get().getInfo().getEmail());
      assertEquals("My User", userProfile.get().getInfo().getFullName());
      assertEquals("example.com", userProfile.get().getInfo().getDomain());
      assertEquals(UserTypeProto.INTERNAL, userProfile.get().getInfo().getType());
    }

    @Test
    @DisplayName("Returns an empty optional if the token is not authenticated")
    void getUserProfile_testTokenNotAuthenticated() {
      when(userManagementStub.getUserMyself(any(GetUserMyselfRequest.class)))
          .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

      Optional<UserMyselfProto> userProfile =
          userManagementAuthenticationService.getUserMyself("tokenz");

      verify(userManagementStub, times(1)).getUserMyself(any(GetUserMyselfRequest.class));

      assertTrue(userProfile.isEmpty());
    }
  }
}
