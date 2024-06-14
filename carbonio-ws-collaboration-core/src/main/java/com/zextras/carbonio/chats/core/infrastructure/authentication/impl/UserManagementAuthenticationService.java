// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserMyself;
import io.vavr.control.Try;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;

@Singleton
public class UserManagementAuthenticationService implements AuthenticationService {

  private final UserManagementClient userManagementClient;

  @Inject
  public UserManagementAuthenticationService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<String> validateCredentials(String authToken) {
    return Optional.ofNullable(authToken)
        .map(
            token ->
                userManagementClient
                    .validateUserToken(token)
                    .map(UserId::getUserId)
                    .getOrElse(() -> null));
  }

  public Try<UserMyself> getUserMySelf(String authToken) {
    return userManagementClient.getUserMyself(
        String.format("%s=%s", AuthenticationMethod.ZM_AUTH_TOKEN.name(), authToken));
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
