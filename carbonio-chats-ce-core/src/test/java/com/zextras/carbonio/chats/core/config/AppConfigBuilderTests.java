// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import com.zextras.carbonio.chats.core.config.impl.MockAppConfig;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class AppConfigBuilderTests {

  @Nested
  @DisplayName("Builds app configs tests")
  public class BuildsAppConfigsTests {

    @Test
    @DisplayName("Correctly adds all configuration resolvers")
    public void addAllConfigurationResolvers() {
      AppConfigBuilder builder = AppConfigBuilder.create()
        .add(MockAppConfig.create(AppConfigType.ENVIRONMENT).setLoaded(true).set(ConfigName.HIKARI_IDLE_TIMEOUT, "300"))
        .add(MockAppConfig.create(AppConfigType.PROPERTY).setLoaded(true).set(ConfigName.HIKARI_MIN_POOL_SIZE, "10"))
        .add(MockAppConfig.create(AppConfigType.CONSUL).setLoaded(true).set(ConfigName.HIKARI_MAX_POOL_SIZE, "50"));

      assertNotNull(builder.getAppConfig());

      Optional<Integer> hikariIdleTimeout = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_IDLE_TIMEOUT);
      assertTrue(hikariIdleTimeout.isPresent());
      assertEquals(300, hikariIdleTimeout.get());

      Optional<Integer> hikariMinPoolSize = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_MIN_POOL_SIZE);
      assertTrue(hikariMinPoolSize.isPresent());
      assertEquals(10, hikariMinPoolSize.get());

      Optional<Integer> hikariMaxPoolSize = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_MAX_POOL_SIZE);
      assertTrue(hikariMaxPoolSize.isPresent());
      assertEquals(50, hikariMaxPoolSize.get());
    }

    @Test
    @DisplayName("Correctly skip a null configuration resolver")
    public void skipNullConfigurationResolver() {
      AppConfigBuilder builder = AppConfigBuilder.create()
        .add(MockAppConfig.create(AppConfigType.ENVIRONMENT).setLoaded(true).set(ConfigName.HIKARI_IDLE_TIMEOUT, "300"))
        .add(null)
        .add(MockAppConfig.create(AppConfigType.CONSUL).setLoaded(true).set(ConfigName.HIKARI_MAX_POOL_SIZE, "50"));

      assertNotNull(builder.getAppConfig());

      Optional<Integer> hikariIdleTimeout = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_IDLE_TIMEOUT);
      assertTrue(hikariIdleTimeout.isPresent());
      assertEquals(300, hikariIdleTimeout.get());

      Optional<Integer> hikariMaxPoolSize = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_MAX_POOL_SIZE);
      assertTrue(hikariMaxPoolSize.isPresent());
      assertEquals(50, hikariMaxPoolSize.get());
    }

    @Test
    @DisplayName("Correctly skip a unloaded configuration resolver")
    public void skipUnloadedConfigurationResolver() {
      AppConfigBuilder builder = AppConfigBuilder.create()
        .add(MockAppConfig.create(AppConfigType.ENVIRONMENT).setLoaded(true).set(ConfigName.HIKARI_IDLE_TIMEOUT, "300"))
        .add(MockAppConfig.create(AppConfigType.PROPERTY).setLoaded(false).set(ConfigName.HIKARI_MIN_POOL_SIZE, "10"))
        .add(MockAppConfig.create(AppConfigType.CONSUL).setLoaded(true).set(ConfigName.HIKARI_MAX_POOL_SIZE, "50"));

      assertNotNull(builder.getAppConfig());

      Optional<Integer> hikariIdleTimeout = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_IDLE_TIMEOUT);
      assertTrue(hikariIdleTimeout.isPresent());
      assertEquals(300, hikariIdleTimeout.get());

      Optional<Integer> hikariMinPoolSize = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_MIN_POOL_SIZE);
      assertTrue(hikariMinPoolSize.isEmpty());

      Optional<Integer> hikariMaxPoolSize = builder.getAppConfig().get(Integer.class, ConfigName.HIKARI_MAX_POOL_SIZE);
      assertTrue(hikariMaxPoolSize.isPresent());
      assertEquals(50, hikariMaxPoolSize.get());
    }

    @Test
    @DisplayName("Not builds an AppConfig when no configuration resolver is valid")
    public void noConfigurationResolverValid() {
      AppConfigBuilder builder = AppConfigBuilder.create()
        .add(null)
        .add(MockAppConfig.create(AppConfigType.PROPERTY).setLoaded(false).set(ConfigName.HIKARI_MIN_POOL_SIZE, "10"))
        .add(MockAppConfig.create(AppConfigType.CONSUL).setLoaded(false).set(ConfigName.HIKARI_MAX_POOL_SIZE, "50"));

      assertNull(builder.getAppConfig());
    }
  }
}
