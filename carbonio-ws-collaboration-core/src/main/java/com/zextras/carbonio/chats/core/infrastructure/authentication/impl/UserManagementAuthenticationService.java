// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
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

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
