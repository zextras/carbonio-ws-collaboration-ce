// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.config;

import com.zextras.carbonio.ws.plugin.api.ConsulKeyMapping;
import io.quarkus.runtime.ExecutionMode;
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
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Plugins contribute keys via ServiceLoader, so a jar dropped into plugins/ (B1) brings its own
 * Consul mappings once it is on the classpath.
 *
 * <p><strong>Runtime only.</strong> SmallRye discovers this factory through ServiceLoader during
 * augmentation as well, and Quarkus records the runtime values it sees from non-env/sysprop sources
 * as defaults in {@code generated-bytecode.jar} ({@code RunTimeConfigCustomizer}). With Consul
 * reachable at (re-)augmentation time — B1's postinst rebuild — DB/RabbitMQ credentials and the Janus
 * api-secret would be baked into the jar. The factory therefore returns no source unless the
 * application lifecycle has entered runtime init: the generated {@code ApplicationImpl.doStart}
 * calls {@code ExecutionModeManager.runtimeInit()} right before building the runtime config, while
 * augmentation never runs the application and leaves the mode {@code UNSET} (native static init is
 * {@code STATIC_INIT} and this factory is only registered for the runtime config anyway).
 */
public class ConsulKvConfigSourceFactory implements ConfigSourceFactory {

  @Override
  public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
    return getConfigSources(context, System::getenv);
  }

  Iterable<ConfigSource> getConfigSources(ConfigSourceContext context, Function<String, String> env) {
    return getConfigSources(context, env, ConsulKvConfigSourceFactory::isApplicationRuntime);
  }

  Iterable<ConfigSource> getConfigSources(
      ConfigSourceContext context, Function<String, String> env, BooleanSupplier applicationRuntime) {
    if (!applicationRuntime.getAsBoolean()) {
      return List.of();
    }
    if (!Boolean.parseBoolean(value(context, env, "ws.consul.enabled", "WS_CONSUL_ENABLED", "false"))) {
      return List.of();
    }
    Map<String, String> mappings =
        mergeMappings(ServiceLoader.load(ConsulKeyMapping.class, Thread.currentThread().getContextClassLoader()));
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).build();
    return List.of(ConsulKvConfigSource.load(
        value(context, env, "ws.consul.url", "WS_CONSUL_URL", "http://localhost:8500"),
        value(context, env, "CONSUL_HTTP_TOKEN", "CONSUL_HTTP_TOKEN", null),
        mappings,
        http));
  }

  /** True only once the application lifecycle runs (never during build-time augmentation). */
  static boolean isApplicationRuntime() {
    ExecutionMode mode = ExecutionMode.current();
    return mode == ExecutionMode.RUNTIME_INIT || mode == ExecutionMode.RUNNING;
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

  /**
   * The context may only expose sources below this factory's priority, which would hide system
   * properties and environment variables; so those are read directly: system property, then
   * environment, then context, then default.
   */
  private static String value(
      ConfigSourceContext context, Function<String, String> env, String name, String envName, String defaultValue) {
    String fromSystem = System.getProperty(name);
    if (fromSystem != null) {
      return fromSystem;
    }
    String fromEnv = env.apply(envName);
    if (fromEnv != null) {
      return fromEnv;
    }
    ConfigValue value = context.getValue(name);
    return value == null || value.getValue() == null ? defaultValue : value.getValue();
  }
}
