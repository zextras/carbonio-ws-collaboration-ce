// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.cache.CacheHandler;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserMyself;

import java.util.Optional;

@Singleton
public class UserManagementAuthenticationService implements AuthenticationService {

  private final UserManagementClient userManagementClient;
  private final CacheHandler cacheHandler;

  @Inject
  public UserManagementAuthenticationService(
      UserManagementClient userManagementClient, CacheHandler cacheHandler) {
    this.userManagementClient = userManagementClient;
    this.cacheHandler = cacheHandler;
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

  public Optional<UserMyself> getUserMyself(String authToken) {
    if (authToken == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        cacheHandler.getUserMyselfCache().get(authToken, this::fetchUserMyself));
  }

  private UserMyself fetchUserMyself(String authToken) {
    return userManagementClient
        .getUserMyself(String.format("%s=%s", AuthenticationMethod.ZM_AUTH_TOKEN.name(), authToken))
        .onFailure(
            throwable ->
                ChatsLogger.warn(
                    "Authentication failed for token "
                        + authToken
                        + "\n "
                        + throwable.getMessage()))
        .toJavaOptional()
        .orElse(null);
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}