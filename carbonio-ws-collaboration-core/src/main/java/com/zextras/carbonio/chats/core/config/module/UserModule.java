// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static com.zextras.carbonio.chats.core.config.module.CoreModule.URL_PATTERN;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zextras.carbonio.chats.api.AuthApi;
import com.zextras.carbonio.chats.api.AuthApiService;
import com.zextras.carbonio.chats.api.UsersApi;
import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.authentication.impl.UserManagementAuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.impl.UserManagementProfilingService;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanUserRepository;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.service.impl.CapabilityServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.UserServiceImpl;
import com.zextras.carbonio.chats.core.web.api.AuthApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.UsersApiServiceImpl;
import com.zextras.carbonio.user_management.sdk.rest.ApiClient;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import java.time.Duration;

public class UserModule extends AbstractModule {

  // Without it the generated ApiClient has no request timeout at all.
  private static final Duration USER_MANAGEMENT_TIMEOUT = Duration.ofMillis(5000);

  @Override
  protected void configure() {
    bind(UsersApi.class);
    bind(UsersApiService.class).to(UsersApiServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);
    bind(UserRepository.class).to(EbeanUserRepository.class);
    bind(CapabilityService.class).to(CapabilityServiceImpl.class);

    bind(AuthApi.class);
    bind(AuthApiService.class).to(AuthApiServiceImpl.class);

    bind(ProfilingService.class).to(UserManagementProfilingService.class);
    bind(AuthenticationService.class).to(UserManagementAuthenticationService.class);
  }

  @Singleton
  @Provides
  @Named("userManagementBaseUrl")
  private String getUserManagementBaseUrl(AppConfig appConfig) {
    return String.format(
        URL_PATTERN,
        appConfig.get(String.class, ConfigName.USER_MANAGEMENT_HOST).orElseThrow(),
        appConfig.get(String.class, ConfigName.USER_MANAGEMENT_PORT).orElseThrow());
  }

  @Singleton
  @Provides
  private UserResourceApi getUserResourceApi(
      @Named("userManagementBaseUrl") String userManagementBaseUrl) {
    // Pin HTTP/1.1: the User Management service does not support h2c cleartext upgrade.
    java.net.http.HttpClient.Builder httpClientBuilder =
        java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1);
    ApiClient apiClient =
        new ApiClient(
            httpClientBuilder, ApiClient.createDefaultObjectMapper(), userManagementBaseUrl);
    // Set before new UserResourceApi(): its constructor copies the timeouts.
    apiClient.setConnectTimeout(USER_MANAGEMENT_TIMEOUT);
    apiClient.setReadTimeout(USER_MANAGEMENT_TIMEOUT);
    return new UserResourceApi(apiClient);
  }
}
