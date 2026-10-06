// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.net.http.HttpClient;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserManagementProducer {

  static final Duration TIMEOUT = Duration.ofMillis(5000);

  @Produces
  @Singleton
  UserResourceApi userResourceApi(@ConfigProperty(name = "ws.user-management.url") String url) {
    // Pin HTTP/1.1: User Management does not support h2c cleartext upgrade.
    HttpClient.Builder builder =
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(TIMEOUT);
    com.zextras.carbonio.user_management.sdk.rest.ApiClient apiClient =
        new com.zextras.carbonio.user_management.sdk.rest.ApiClient(
            builder,
            com.zextras.carbonio.user_management.sdk.rest.ApiClient.createDefaultObjectMapper(),
            url);
    // Before constructing the API: its constructor snapshots the timeouts into final fields.
    apiClient.setReadTimeout(TIMEOUT);
    return new UserResourceApi(apiClient);
  }
}
