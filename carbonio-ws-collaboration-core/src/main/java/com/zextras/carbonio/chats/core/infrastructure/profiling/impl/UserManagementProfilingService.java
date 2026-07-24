// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.ProfilingException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;

@Singleton
public class UserManagementProfilingService implements ProfilingService {

  private static final String HEALTH_LIVE_PATH = "/q/health/live";
  private static final int HTTP_NOT_FOUND = 404;

  private final UserResourceApi userResourceApi;
  private final HttpClient httpClient;
  private final String userManagementBaseUrl;

  @Inject
  public UserManagementProfilingService(
      UserResourceApi userResourceApi,
      HttpClient httpClient,
      @Named("userManagementBaseUrl") String userManagementBaseUrl) {
    this.userResourceApi = userResourceApi;
    this.httpClient = httpClient;
    this.userManagementBaseUrl = userManagementBaseUrl;
  }

  @Override
  public Optional<UserProfile> getById(UserPrincipal principal, UUID userId) {
    principal.getAuthToken().orElseThrow(ForbiddenException::new);
    try {
      UserInfoDto userInfo = userResourceApi.internalUsersIdUserIdGet(userId.toString());
      return Optional.of(mapToUserProfile(userInfo));
    } catch (ApiException e) {
      if (e.getCode() == HTTP_NOT_FOUND) {
        return Optional.empty();
      }
      throw new ProfilingException(e);
    }
  }

  @Override
  public List<UserProfile> getByIds(UserPrincipal principal, List<String> userIds) {
    principal.getAuthToken().orElseThrow(ForbiddenException::new);
    try {
      return userResourceApi.internalUsersPost(userIds).stream()
          .map(this::mapToUserProfile)
          .toList();
    } catch (ApiException e) {
      throw new ProfilingException(e);
    }
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

  private UserProfile mapToUserProfile(UserInfoDto userInfo) {
    return UserProfile.create(userInfo.getUserId())
        .name(userInfo.getFullName())
        .email(userInfo.getEmail())
        .domain(userInfo.getDomain())
        .type(UserType.from(userInfo.getType()));
  }
}
