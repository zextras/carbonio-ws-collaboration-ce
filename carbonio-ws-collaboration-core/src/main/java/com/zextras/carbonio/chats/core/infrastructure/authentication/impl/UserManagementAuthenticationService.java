// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Optional;

@Singleton
public class UserManagementAuthenticationService implements AuthenticationService {

  private final UserManagementServiceBlockingStub userManagementStub;
  private final ManagedChannel userManagementChannel;

  @Inject
  public UserManagementAuthenticationService(
      UserManagementServiceBlockingStub userManagementStub,
      @Named("userManagementChannel") ManagedChannel userManagementChannel) {
    this.userManagementStub = userManagementStub;
    this.userManagementChannel = userManagementChannel;
  }

  @Override
  public Optional<String> validateCredentials(String authToken) {
    if (authToken == null) {
      return Optional.empty();
    }
    try {
      GetUserMyselfRequest request =
          GetUserMyselfRequest.newBuilder().setToken(authToken).build();
      UserMyselfResponse response = userManagementStub.getUserMyself(request);
      return Optional.ofNullable(response.getUser().getInfo().getUserId());
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
        return Optional.empty();
      }
      ChatsLogger.warn(
          "Credential validation failed for token " + authToken + "\n " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<UserMyselfProto> getUserMyself(String authToken) {
    if (authToken == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(fetchUserMyself(authToken));
  }

  private UserMyselfProto fetchUserMyself(String authToken) {
    try {
      GetUserMyselfRequest request =
          GetUserMyselfRequest.newBuilder().setToken(authToken).build();
      UserMyselfResponse response = userManagementStub.getUserMyself(request);
      return response.getUser();
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() != Status.Code.UNAUTHENTICATED) {
        ChatsLogger.warn(
            "Authentication failed for token " + authToken + "\n " + e.getMessage());
      }
      return null;
    }
  }

  @Override
  public boolean isAlive() {
    ConnectivityState state = userManagementChannel.getState(true);
    return state == ConnectivityState.READY || state == ConnectivityState.IDLE;
  }
}
