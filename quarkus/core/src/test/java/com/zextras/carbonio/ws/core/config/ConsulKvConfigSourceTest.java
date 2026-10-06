// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zextras.carbonio.ws.plugin.api.ConsulKeyMapping;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsulKvConfigSourceTest {

  WireMockServer wm;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(options().dynamicPort());
    wm.start();
  }

  @AfterEach
  void tearDown() {
    wm.stop();
  }

  @Test
  void mappedKeysAreReadRaw() {
    wm.stubFor(get(urlPathEqualTo("/v1/kv/carbonio-videoserver/api-secret"))
        .withQueryParam("raw", equalTo("true"))
        .willReturn(aResponse().withStatus(200).withBody("s3cret\n")));
    wm.stubFor(get(urlPathEqualTo("/v1/kv/missing/key")).willReturn(notFound()));

    var source = ConsulKvConfigSource.load(wm.baseUrl(), "tok",
        Map.of("ws.videoserver.api-secret", "carbonio-videoserver/api-secret", "ws.other", "missing/key"),
        HttpClient.newHttpClient());

    assertThat(source.getValue("ws.videoserver.api-secret")).isEqualTo("s3cret");
    assertThat(source.getValue("ws.other")).isNull();
    assertThat(source.getOrdinal()).isEqualTo(270);
    wm.verify(getRequestedFor(urlPathEqualTo("/v1/kv/carbonio-videoserver/api-secret"))
        .withHeader("X-Consul-Token", equalTo("tok")));
  }

  @Test
  void unreachableConsulYieldsAnEmptySource() {
    var source = ConsulKvConfigSource.load("http://127.0.0.1:1", null,
        Map.of("ws.videoserver.api-secret", "carbonio-videoserver/api-secret"), HttpClient.newHttpClient());
    assertThat(source.getProperties()).isEmpty();
  }

  @Test
  void aPropertyMappedTwiceFailsFast() {
    ConsulKeyMapping a = () -> Map.of("ws.x", "a/x");
    ConsulKeyMapping b = () -> Map.of("ws.x", "b/x");
    assertThatThrownBy(() -> ConsulKvConfigSourceFactory.mergeMappings(List.of(a, b)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ws.x");
  }

  @Test
  void coreMappingsCoverCredentialsAndSecrets() {
    assertThat(new CoreConsulKeys().consulKeys())
        .containsEntry("ws.videoserver.api-secret", "carbonio-videoserver/api-secret")
        .containsEntry("quarkus.datasource.password", "carbonio-ws-collaboration-db/db-password")
        .containsEntry("ws.rabbitmq.password", "carbonio-message-broker/default/password");
  }
}
