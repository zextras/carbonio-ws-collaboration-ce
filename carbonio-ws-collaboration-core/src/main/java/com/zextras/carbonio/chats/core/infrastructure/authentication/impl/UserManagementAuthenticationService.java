// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.apache.http.client.methods.CloseableHttpResponse;

@Singleton
public class UserManagementAuthenticationService implements AuthenticationService {

  private static final String HEALTH_LIVE_PATH = "/q/health/live";
  private static final int HTTP_UNAUTHORIZED = 401;

  private final UserResourceApi userResourceApi;
  private final HttpClient httpClient;
  private final String userManagementBaseUrl;

  @Inject
  public UserManagementAuthenticationService(
      UserResourceApi userResourceApi,
      HttpClient httpClient,
      @Named("userManagementBaseUrl") String userManagementBaseUrl) {
    this.userResourceApi = userResourceApi;
    this.httpClient = httpClient;
    this.userManagementBaseUrl = userManagementBaseUrl;
  }

  @Override
  public Optional<String> validateCredentials(String authToken) {
    if (authToken == null) {
      return Optional.empty();
    }
    try {
      MyselfDto myself = userResourceApi.internalUsersMyselfGet(cookieHeader(authToken));
      return Optional.ofNullable(myself.getInfo().getUserId());
    } catch (ApiException e) {
      if (e.getCode() == HTTP_UNAUTHORIZED) {
        return Optional.empty();
      }
      ChatsLogger.warn(
          "Credential validation failed for token " + authToken + "\n " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<MyselfDto> getUserMyself(String authToken) {
    if (authToken == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(fetchUserMyself(authToken));
  }

  private MyselfDto fetchUserMyself(String authToken) {
    try {
      return userResourceApi.internalUsersMyselfGet(cookieHeader(authToken));
    } catch (ApiException e) {
      if (e.getCode() != HTTP_UNAUTHORIZED) {
        ChatsLogger.warn("Authentication failed for token " + authToken + "\n " + e.getMessage());
      }
      return null;
    }
  }

  private Map<String, String> cookieHeader(String authToken) {
    return Map.of("Cookie", "ZM_AUTH_TOKEN=" + authToken);
  }

  @Override
  public boolean isAlive() {
    try (CloseableHttpResponse response =
        httpClient.sendGet(userManagementBaseUrl + HEALTH_LIVE_PATH, Map.of())) {
      return response.getStatusLine().getStatusCode() == 200;
    } catch (IOException e) {
      return false;
    }
  }
}
