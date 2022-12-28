// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserManagementAuthenticationService implements AuthenticationService {

  private final UserManagementClient userManagementClient;

  @Inject
  public UserManagementAuthenticationService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<String> validateCredentials(Map<AuthenticationMethod, String> credentials) {
    return Optional.ofNullable(credentials)
      .map(credentialMap -> credentialMap.get(AuthenticationMethod.ZM_AUTH_TOKEN))
      .map(token -> userManagementClient.validateUserToken(token).map(UserId::getUserId).getOrElse(() -> null));
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
