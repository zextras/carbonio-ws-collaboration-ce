// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.exceptions.UserNotFound;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UserManagementProfilingService implements ProfilingService {

  private final UserManagementClient userManagementClient;

  @Inject
  public UserManagementProfilingService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<UserProfile> getById(UserPrincipal principal, UUID userId) {
    String token = principal.getAuthToken().orElseThrow(ForbiddenException::new);
    return Optional.ofNullable(
        userManagementClient
            .getUserById(
                String.format("%s=%s", AuthenticationMethod.ZM_AUTH_TOKEN.name(), token),
                userId.toString())
            .map(
                u ->
                    UserProfile.create(u.getId().getUserId())
                        .name(u.getFullName())
                        .email(u.getEmail())
                        .domain(u.getDomain())
                        .type(UserType.valueOf(u.getType().name())))
            .recover(UserNotFound.class, e -> null)
            .getOrElseThrow(fail -> new ProfilingException(fail)));
  }

  @Override
  public List<UserProfile> getByIds(UserPrincipal principal, List<String> userIds) {
    String token = principal.getAuthToken().orElseThrow(ForbiddenException::new);
    return userManagementClient
        .getUsers(String.join("=", AuthenticationMethod.ZM_AUTH_TOKEN.name(), token), userIds)
        .getOrElseThrow(fail -> new ProfilingException(fail))
        .stream()
        .map(
            u ->
                UserProfile.create(u.getId().getUserId())
                    .name(u.getFullName())
                    .email(u.getEmail())
                    .domain(u.getDomain())
                    .type(UserType.valueOf(u.getType().name())))
        .toList();
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
