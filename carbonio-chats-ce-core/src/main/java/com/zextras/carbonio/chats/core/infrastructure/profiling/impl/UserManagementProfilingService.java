// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.exceptions.UserNotFound;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserManagementProfilingService implements ProfilingService {

  private static final String AUTH_COOKIE = "ZM_AUTH_TOKEN";

  private final UserManagementClient userManagementClient;

  @Inject
  public UserManagementProfilingService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<UserProfile> getById(UserPrincipal principal, UUID userId) {
    String token = principal.getAuthCredentialFor(AuthenticationMethod.ZM_AUTH_TOKEN)
      .orElseThrow(ForbiddenException::new);
    return Optional.ofNullable(
      userManagementClient.getUserByUUID(String.format("%s=%s", AUTH_COOKIE, token), userId).map(userInfo ->
        UserProfile.create(userInfo.getId())
          .name(userInfo.getFullName())
          .email(userInfo.getEmail())
          .domain(userInfo.getDomain())
      ).recover(UserNotFound.class, e -> null)
        .getOrElseThrow((fail) -> new ProfilingException(fail)));
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
