// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceDiscoveryImplTest {

  WireMockServer wm;
  ServiceDiscoveryImpl discovery;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(options().dynamicPort());
    wm.start();
    discovery = new ServiceDiscoveryImpl(true, wm.baseUrl(), Optional.of("tok"), Optional.empty());
  }

  @AfterEach
  void tearDown() {
    wm.stop();
  }

  @Test
  void passingInstancesMeanAvailable() {
    wm.stubFor(get(urlPathEqualTo("/v1/health/service/carbonio-turn-server-auth"))
        .withQueryParam("passing", equalTo("true"))
        .willReturn(okJson("[{\"Service\":{\"ID\":\"t1\"}}]")));
    assertThat(discovery.isAvailable("carbonio-turn-server-auth")).isTrue();
    wm.verify(getRequestedFor(urlPathEqualTo("/v1/health/service/carbonio-turn-server-auth"))
        .withHeader("X-Consul-Token", equalTo("tok")));
  }

  @Test
  void noPassingInstanceMeansUnavailable() {
    wm.stubFor(get(urlPathEqualTo("/v1/health/service/x")).willReturn(okJson("[]")));
    assertThat(discovery.isAvailable("x")).isFalse();
  }

  @Test
  void consulErrorMeansUnavailable() {
    wm.stubFor(get(urlPathEqualTo("/v1/health/service/x")).willReturn(serverError()));
    assertThat(discovery.isAvailable("x")).isFalse();
  }

  @Test
  void disabledConsulUsesTheStaticList() {
    var local = new ServiceDiscoveryImpl(false, "http://unused", Optional.empty(), Optional.of(List.of("a")));
    assertThat(local.isAvailable("a")).isTrue();
    assertThat(local.isAvailable("b")).isFalse();
  }
}
