// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcherClient;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.MessageDispatcherHttpClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerConfig;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerConfigImpl;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerHttpClient;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.preview.sdk.PreviewClient;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.storages.api.StoragesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Base64;

/** CDI port of the {@code @Provides} methods of CE's Guice {@code CoreModule} (non-RabbitMQ). */
@ApplicationScoped
public class CoreProducers {

  private static final String URL_PATTERN = "http://%s:%s";

  // 5s connect + read timeout: this codebase's existing convention for a shared client calling
  // another Carbonio service over the mesh (see HttpClientProvider.TIMEOUT_MILLIS, also 5000ms).
  private static final Duration USER_MANAGEMENT_TIMEOUT = Duration.ofMillis(5000);

  @Produces
  @Singleton
  Clock clock() {
    return Clock.system(ZoneId.systemDefault());
  }

  @Produces
  @Singleton
  StoragesClient storagesClient(AppConfig appConfig) {
    return StoragesClient.atUrl(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.STORAGES_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.STORAGES_PORT).orElseThrow()));
  }

  @Produces
  @Singleton
  PreviewClient previewClient(AppConfig appConfig) {
    return PreviewClient.atURL(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.PREVIEWER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.PREVIEWER_PORT).orElseThrow()));
  }

  @Produces
  @Singleton
  @Named("userManagementBaseUrl")
  String userManagementBaseUrl(AppConfig appConfig) {
    return String.format(
        URL_PATTERN,
        appConfig.get(String.class, ConfigName.USER_MANAGEMENT_HOST).orElseThrow(),
        appConfig.get(String.class, ConfigName.USER_MANAGEMENT_PORT).orElseThrow());
  }

  @Produces
  @Singleton
  UserResourceApi userResourceApi(@Named("userManagementBaseUrl") String userManagementBaseUrl) {
    // Pin HTTP/1.1: the User Management service does not support h2c cleartext upgrade.
    java.net.http.HttpClient.Builder httpClientBuilder =
        java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .connectTimeout(USER_MANAGEMENT_TIMEOUT);
    com.zextras.carbonio.user_management.sdk.rest.ApiClient apiClient =
        new com.zextras.carbonio.user_management.sdk.rest.ApiClient(
            httpClientBuilder,
            com.zextras.carbonio.user_management.sdk.rest.ApiClient.createDefaultObjectMapper(),
            userManagementBaseUrl);
    // Must be set before constructing UserResourceApi: its constructor snapshots the ApiClient's
    // timeouts into final fields, so setting them afterwards would be a silent no-op.
    apiClient.setConnectTimeout(USER_MANAGEMENT_TIMEOUT);
    apiClient.setReadTimeout(USER_MANAGEMENT_TIMEOUT);
    return new UserResourceApi(apiClient);
  }

  @Produces
  @Singleton
  MessageDispatcherCredentials messageDispatcherCredentials(AppConfig appConfig) {
    return new MessageDispatcherCredentials(
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_HOST).orElseThrow(),
        appConfig.get(Integer.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_PORT).orElseThrow(),
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME).orElse(null),
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_USERNAME).orElse(null),
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_PASSWORD).orElse(null));
  }

  @Produces
  @Singleton
  MessageDispatcherClient messageDispatcherClient(
      AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
    String mongooseimUrl =
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.XMPP_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.XMPP_SERVER_HTTP_PORT).orElseThrow());
    String authToken =
        Base64.getEncoder()
            .encodeToString(
                String.join(
                        ":",
                        appConfig.get(String.class, ConfigName.XMPP_SERVER_USERNAME).orElseThrow(),
                        appConfig.get(String.class, ConfigName.XMPP_SERVER_PASSWORD).orElseThrow())
                    .getBytes());
    return new MessageDispatcherHttpClient(httpClient, mongooseimUrl, authToken, objectMapper);
  }

  @Produces
  @Singleton
  VideoServerClient videoServerClient(
      AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
    return new VideoServerHttpClient(
        httpClient,
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow()),
        objectMapper);
  }

  @Produces
  @Singleton
  VideoServerConfig videoServerConfig(AppConfig appConfig) {
    return new VideoServerConfigImpl()
        .apiSecret(appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN).orElse(null))
        .bitrate(appConfig.get(Integer.class, ConfigName.VIDEO_ROOM_BITRATE).orElse(8000000))
        .bitrateCap(appConfig.get(Boolean.class, ConfigName.VIDEO_ROOM_BITRATE_CAP).orElse(true));
  }
}
