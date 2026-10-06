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

  @Test
  void stopsAfterFirstConnectionFailure() {
    java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
    HttpClient failing = new HttpClient() {
      public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
      public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
      public Redirect followRedirects() { return Redirect.NEVER; }
      public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
      public javax.net.ssl.SSLContext sslContext() { return null; }
      public javax.net.ssl.SSLParameters sslParameters() { return null; }
      public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
      public Version version() { return Version.HTTP_1_1; }
      public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }
      public <T> java.net.http.HttpResponse<T> send(java.net.http.HttpRequest r,
          java.net.http.HttpResponse.BodyHandler<T> h) throws java.io.IOException {
        attempts.incrementAndGet();
        throw new java.io.IOException(new java.net.ConnectException("refused"));
      }
      public <T> java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
          java.net.http.HttpRequest r, java.net.http.HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
      public <T> java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
          java.net.http.HttpRequest r, java.net.http.HttpResponse.BodyHandler<T> h,
          java.net.http.HttpResponse.PushPromiseHandler<T> p) { throw new UnsupportedOperationException(); }
    };
    var source = ConsulKvConfigSource.load("http://consul", null,
        Map.of("a", "k/a", "b", "k/b", "c", "k/c"), failing);
    assertThat(attempts).hasValue(1);
    assertThat(source.getProperties()).isEmpty();
  }

  private static io.smallrye.config.ConfigSourceContext ctx(Map<String, String> values) {
    return new io.smallrye.config.ConfigSourceContext() {
      public io.smallrye.config.ConfigValue getValue(String name) {
        return io.smallrye.config.ConfigValue.builder().withName(name).withValue(values.get(name)).build();
      }

      public java.util.Iterator<String> iterateNames() {
        return values.keySet().iterator();
      }
    };
  }

  @Test
  void factoryIsDisabledByDefault() {
    var sources = new ConsulKvConfigSourceFactory().getConfigSources(ctx(Map.of()), n -> null);
    assertThat(sources).isEmpty();
  }

  @Test
  void systemPropertyEnablesFactoryEvenIfContextSaysFalse() {
    wm.stubFor(get(urlPathEqualTo("/v1/kv/carbonio-videoserver/api-secret"))
        .willReturn(aResponse().withStatus(200).withBody("fromSys")));
    System.setProperty("ws.consul.enabled", "true");
    System.setProperty("ws.consul.url", wm.baseUrl());
    try {
      var sources = new ConsulKvConfigSourceFactory().getConfigSources(
          ctx(Map.of("ws.consul.enabled", "false")), n -> null);
      assertThat(sources).hasSize(1);
      assertThat(sources.iterator().next().getValue("ws.videoserver.api-secret")).isEqualTo("fromSys");
    } finally {
      System.clearProperty("ws.consul.enabled");
      System.clearProperty("ws.consul.url");
    }
  }

  @Test
  void environmentEnablesFactoryAndSuppliesToken() {
    wm.stubFor(get(urlPathEqualTo("/v1/kv/carbonio-videoserver/api-secret"))
        .willReturn(aResponse().withStatus(200).withBody("x")));
    Map<String, String> env = Map.of(
        "WS_CONSUL_ENABLED", "true", "WS_CONSUL_URL", wm.baseUrl(), "CONSUL_HTTP_TOKEN", "envtok");
    new ConsulKvConfigSourceFactory().getConfigSources(ctx(Map.of()), env::get);
    wm.verify(getRequestedFor(urlPathEqualTo("/v1/kv/carbonio-videoserver/api-secret"))
        .withHeader("X-Consul-Token", equalTo("envtok")));
  }
}
