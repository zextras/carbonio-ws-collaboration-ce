// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.testsupport;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

/** One WireMock server impersonating user-management, Janus, turn-server and videorecorder. */
public class WireMockResource implements QuarkusTestResourceLifecycleManager {

  private static WireMockServer server;

  public static WireMockServer server() {
    return server;
  }

  @Override
  public Map<String, String> start() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
    String port = String.valueOf(server.port());
    return Map.of(
        "ws.user-management.url", server.baseUrl(),
        "ws.videoserver.url", server.baseUrl(),
        "ws.turn-server.host", "localhost",
        "ws.turn-server.port", port,
        "ws.video-recorder.host", "localhost",
        "ws.video-recorder.port", port);
  }

  @Override
  public void stop() {
    if (server != null) {
      server.stop();
    }
  }
}
