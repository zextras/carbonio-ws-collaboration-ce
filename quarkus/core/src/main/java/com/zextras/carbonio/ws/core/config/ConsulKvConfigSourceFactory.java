// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.config;

import com.zextras.carbonio.ws.plugin.api.ConsulKeyMapping;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Plugins contribute keys via ServiceLoader, so a jar dropped into plugins/ (B1) brings its own
 * Consul mappings once it is on the classpath.
 */
public class ConsulKvConfigSourceFactory implements ConfigSourceFactory {

  @Override
  public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
    if (!Boolean.parseBoolean(value(context, "ws.consul.enabled", "false"))) {
      return List.of();
    }
    Map<String, String> mappings =
        mergeMappings(ServiceLoader.load(ConsulKeyMapping.class, Thread.currentThread().getContextClassLoader()));
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).build();
    return List.of(ConsulKvConfigSource.load(
        value(context, "ws.consul.url", "http://localhost:8500"),
        value(context, "CONSUL_HTTP_TOKEN", null),
        mappings,
        http));
  }

  @Override
  public OptionalInt getPriority() {
    return OptionalInt.of(ConsulKvConfigSource.ORDINAL);
  }

  static Map<String, String> mergeMappings(Iterable<ConsulKeyMapping> contributions) {
    Map<String, String> merged = new HashMap<>();
    for (ConsulKeyMapping contribution : contributions) {
      contribution.consulKeys().forEach((property, key) -> {
        String previous = merged.putIfAbsent(property, key);
        if (previous != null) {
          throw new IllegalStateException(
              "Config property " + property + " mapped twice: " + previous + " and " + key);
        }
      });
    }
    return merged;
  }

  private static String value(ConfigSourceContext context, String name, String defaultValue) {
    ConfigValue value = context.getValue(name);
    return value == null || value.getValue() == null ? defaultValue : value.getValue();
  }
}
