// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserByIdRequest;
import com.zextras.carbonio.user_management.sdk.grpc.GetUsersRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UserManagementProfilingService implements ProfilingService {

  private final UserManagementServiceBlockingStub userManagementStub;
  private final ManagedChannel userManagementChannel;

  @Inject
  public UserManagementProfilingService(
      UserManagementServiceBlockingStub userManagementStub,
      @Named("userManagementChannel") ManagedChannel userManagementChannel) {
    this.userManagementStub = userManagementStub;
    this.userManagementChannel = userManagementChannel;
  }

  @Override
  public Optional<UserProfile> getById(UserPrincipal principal, UUID userId) {
    try {
      UserInfoProto userInfo =
          userManagementStub
              .getUserById(
                  GetUserByIdRequest.newBuilder()
                      .setUserId(userId.toString())
                      .build())
              .getUser();
      return Optional.of(mapToUserProfile(userInfo));
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        return Optional.empty();
      }
      throw new ProfilingException(e);
    }
  }

  @Override
  public List<UserProfile> getByIds(UserPrincipal principal, List<String> userIds) {
    try {
      return userManagementStub
          .getUsers(
              GetUsersRequest.newBuilder().addAllUserIds(userIds).build())
          .getUsersList()
          .stream()
          .map(this::mapToUserProfile)
          .toList();
    } catch (StatusRuntimeException e) {
      throw new ProfilingException(e);
    }
  }

  @Override
  public boolean isAlive() {
    ConnectivityState state = userManagementChannel.getState(true);
    return state == ConnectivityState.READY || state == ConnectivityState.IDLE;
  }

  private UserProfile mapToUserProfile(UserInfoProto userInfo) {
    return UserProfile.create(userInfo.getUserId())
        .name(userInfo.getFullName())
        .email(userInfo.getEmail())
        .domain(userInfo.getDomain())
        .type(UserType.from(userInfo.getType()));
  }
}
