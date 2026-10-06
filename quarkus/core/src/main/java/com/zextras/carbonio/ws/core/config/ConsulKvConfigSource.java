// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

/** Snapshot of the mapped Consul KV keys, read once at startup (no watches in the PoC). */
public final class ConsulKvConfigSource implements ConfigSource {

  static final int ORDINAL = 270;
  private static final System.Logger LOG = System.getLogger(ConsulKvConfigSource.class.getName());

  private final Map<String, String> values;

  private ConsulKvConfigSource(Map<String, String> values) {
    this.values = Map.copyOf(values);
  }

  public static ConsulKvConfigSource load(
      String consulUrl, String token, Map<String, String> propertyToKey, HttpClient http) {
    Map<String, String> values = new HashMap<>();
    for (Map.Entry<String, String> mapping : propertyToKey.entrySet()) {
      try {
        HttpRequest.Builder request =
            HttpRequest.newBuilder(URI.create(consulUrl + "/v1/kv/" + mapping.getValue() + "?raw=true"))
                .timeout(Duration.ofMillis(5000))
                .GET();
        if (token != null && !token.isBlank()) {
          request.header("X-Consul-Token", token);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          values.put(mapping.getKey(), response.body().trim());
        } else if (response.statusCode() != 404) {
          LOG.log(System.Logger.Level.WARNING, "Consul KV {0} answered {1}", mapping.getValue(), response.statusCode());
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        if (isConnectionFailure(e)) {
          LOG.log(System.Logger.Level.WARNING,
              "Consul unreachable at {0} ({1}); skipping remaining keys", consulUrl, e.toString());
          break;
        }
        LOG.log(System.Logger.Level.WARNING, "Consul KV {0} unreadable: {1}", mapping.getValue(), e.toString());
      }
    }
    return new ConsulKvConfigSource(values);
  }

  private static boolean isConnectionFailure(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof java.net.ConnectException || t instanceof java.net.http.HttpConnectTimeoutException) {
        return true;
      }
      if (t.getCause() == t) {
        break;
      }
    }
    return false;
  }

  @Override
  public Map<String, String> getProperties() {
    return values;
  }

  @Override
  public Set<String> getPropertyNames() {
    return values.keySet();
  }

  @Override
  public String getValue(String propertyName) {
    return values.get(propertyName);
  }

  @Override
  public String getName() {
    return "ConsulKvConfigSource";
  }

  @Override
  public int getOrdinal() {
    return ORDINAL;
  }
}
