// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.zextras.carbonio.videorecorder.sdk.rest.api.JobResourceApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.net.http.HttpClient;
import java.time.Duration;

@ApplicationScoped
public class VideoRecorderApiProducer {

  static final Duration TIMEOUT = Duration.ofMillis(5000);

  @Produces
  @Singleton
  JobResourceApi jobResourceApi(RecordingConfig config) {
    HttpClient.Builder builder =
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(TIMEOUT);
    com.zextras.carbonio.videorecorder.sdk.rest.ApiClient apiClient =
        new com.zextras.carbonio.videorecorder.sdk.rest.ApiClient(
            builder,
            com.zextras.carbonio.videorecorder.sdk.rest.ApiClient.createDefaultObjectMapper(),
            "http://" + config.host() + ":" + config.port());
    // Before constructing the API: its constructor snapshots the timeouts into final fields.
    apiClient.setReadTimeout(TIMEOUT);
    return new JobResourceApi(apiClient);
  }
}
