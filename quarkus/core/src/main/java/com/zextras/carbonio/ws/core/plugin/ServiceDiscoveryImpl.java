// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.ws.plugin.api.ServiceDiscovery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ServiceDiscoveryImpl implements ServiceDiscovery {

  private static final Logger LOG = Logger.getLogger(ServiceDiscoveryImpl.class);
  private static final Duration TIMEOUT = Duration.ofMillis(5000);
  private static final ObjectMapper JSON = new ObjectMapper();

  private final boolean consulEnabled;
  private final String consulUrl;
  private final Optional<String> token;
  private final List<String> staticServices;
  private final HttpClient http =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(TIMEOUT).build();

  @Inject
  public ServiceDiscoveryImpl(
      @ConfigProperty(name = "ws.consul.enabled") boolean consulEnabled,
      @ConfigProperty(name = "ws.consul.url") String consulUrl,
      @ConfigProperty(name = "CONSUL_HTTP_TOKEN") Optional<String> token,
      @ConfigProperty(name = "ws.discovery.available-services") Optional<List<String>> staticServices) {
    this.consulEnabled = consulEnabled;
    this.consulUrl = consulUrl;
    this.token = token;
    this.staticServices = staticServices.orElse(List.of());
  }

  @Override
  public boolean isAvailable(String serviceName) {
    if (!consulEnabled) {
      return staticServices.contains(serviceName);
    }
    try {
      HttpRequest.Builder request =
          HttpRequest.newBuilder(URI.create(consulUrl + "/v1/health/service/"
                  + URLEncoder.encode(serviceName, StandardCharsets.UTF_8) + "?passing=true"))
              .timeout(TIMEOUT)
              .GET();
      token.ifPresent(t -> request.header("X-Consul-Token", t));
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200 && JSON.readTree(response.body()).size() > 0;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (Exception e) {
      LOG.warnf("Consul health check for %s failed: %s", serviceName, e.toString());
      return false;
    }
  }
}
