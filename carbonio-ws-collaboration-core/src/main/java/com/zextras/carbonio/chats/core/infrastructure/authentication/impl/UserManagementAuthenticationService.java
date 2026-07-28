// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zextras.carbonio.chats.core.exception.AuthenticationException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
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
      MyselfDto myself = userResourceApi.internalUsersMyselfGet(null, authToken);
      return Optional.ofNullable(myself.getInfo().getUserId());
    } catch (ApiException e) {
      if (e.getCode() == HTTP_UNAUTHORIZED) {
        return Optional.empty();
      }
      // Anything other than a 401 (including code 0 from a network failure/timeout, and 4xx/5xx)
      // is not a real "bad credentials" answer: it means User Management itself could not be
      // reached or misbehaved, so it must surface as a dependency failure, not a silent logout.
      ChatsLogger.warn("Credential validation failed for the provided token\n " + e.getMessage());
      throw new AuthenticationException(e);
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
      return userResourceApi.internalUsersMyselfGet(null, authToken);
    } catch (ApiException e) {
      if (e.getCode() == HTTP_UNAUTHORIZED) {
        return null;
      }
      // Same reasoning as validateCredentials: only a genuine 401 means "not authenticated".
      // Everything else is a dependency failure and must not be swallowed into an anonymous/401
      // response by the caller.
      ChatsLogger.warn("Authentication failed for the provided token\n " + e.getMessage());
      throw new AuthenticationException(e);
    }
  }

  @Override
  public boolean isAlive() {
    try (CloseableHttpResponse response =
        httpClient.sendGet(userManagementBaseUrl + HEALTH_LIVE_PATH, Map.of())) {
      return response.getStatusLine().getStatusCode() == 200;
    } catch (Exception e) {
      // HttpClient wraps connection failures (refused, DNS, timeout) in an unchecked
      // ChatsHttpException instead of the checked IOException declared on sendGet(), so this must
      // catch Exception, not IOException, or a dead User Management takes the whole healthcheck
      // down with it instead of being reported as an unhealthy dependency.
      ChatsLogger.warn("Can't communicate with User Management due to: " + e);
      return false;
    }
  }
}
