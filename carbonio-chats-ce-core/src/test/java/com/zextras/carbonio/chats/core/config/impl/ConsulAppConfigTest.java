package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.io.BaseEncoding;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.ConsulCache.Listener;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.ImmutableValue;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;

@UnitTest
class ConsulAppConfigTest {

  private final AppConfig      appConfig;
  private final Consul         consulClient;
  private final KeyValueClient keyValueClient;

  public ConsulAppConfigTest() {
    Consul.NetworkTimeoutConfig networkTimeoutConfig = mock(Consul.NetworkTimeoutConfig.class);
    when(networkTimeoutConfig.getClientReadTimeoutMillis()).thenReturn(20000);
    keyValueClient = mock(KeyValueClient.class, RETURNS_DEEP_STUBS);

    when(keyValueClient.getNetworkTimeoutConfig()).thenReturn(networkTimeoutConfig);
    consulClient = mock(Consul.class);
    when(consulClient.keyValueClient()).thenReturn(keyValueClient);
    appConfig = Optional.of(ConsulAppConfig.create(consulClient, "TOKEN")).orElseThrow();
  }

  @Nested
  @DisplayName("Load required configurations tests")
  class LoadRequiredConfigurationsTests {

    @Test
    @DisplayName("Correctly retrieves required configurations and cache them")
    public void loadConfigurations_testOk() {
      KVCache kvCache = mock(KVCache.class);
      try (MockedStatic<KVCache> kvCacheMockedStatic = Mockito.mockStatic(KVCache.class)) {
        kvCacheMockedStatic.when((Verification) KVCache.newCache(eq(keyValueClient), eq("carbonio-chats/"), eq(10),
          any(ImmutableQueryOptions.class))).thenReturn(kvCache);
        ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);

        appConfig.load();

        verify(kvCache).addListener(listenerArgumentCaptor.capture());
        Listener<String, Value> listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);
        listener.notify(Map.of(
          "db-username",
          ImmutableValue.builder()
            .key("carbonio-chats/db-username")
            .value(Base64.getEncoder().encodeToString("username".getBytes(StandardCharsets.UTF_8)))
            .createIndex(0L)
            .modifyIndex(0L)
            .lockIndex(0L)
            .flags(0L).build()
        ));
        Optional<String> username = appConfig.get(String.class, ConfigName.DATABASE_USERNAME);
        assertTrue(username.isPresent());
        assertEquals("username", username.get());
      }
    }
  }

  @Nested
  @DisplayName("Get attribute tests")
  class GetAttributesTests {

    @Test
    @DisplayName("Correctly retrieves an attribute")
    public void getAttribute_testOk() {
      Value mockValue = ImmutableValue.builder()
        .key("carbonio-chats/db-password")
        .value(BaseEncoding.base64().encode("testpsw".getBytes(StandardCharsets.UTF_8)))
        .createIndex(0L)
        .modifyIndex(0L)
        .lockIndex(0L)
        .flags(0L)
        .build();
      when(keyValueClient.getValue(eq("carbonio-chats/db-password"), any(QueryOptions.class)))
        .thenReturn(Optional.of(mockValue));
      appConfig.load();
      Optional<String> configValue;
      configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isPresent());
      assertEquals("testpsw", configValue.get());

      configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isPresent());
      assertEquals("testpsw", configValue.get());

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/db-password"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(2)).keyValueClient();
    }

    @Test
    @DisplayName("Returns en empty optional if the attribute was not retrieved")
    public void getAttribute_testNotRetrieved() {
      when(keyValueClient.getValueAsString("carbonio-chats/db-password")).thenReturn(Optional.empty());

      Optional<String> configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isEmpty());

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/db-password"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
    }

    @Test
    @DisplayName("Returns en empty optional if and exception is thrown")
    public void getAttribute_testException() {
      when(keyValueClient.getValueAsString("carbonio-chats/db-password"))
        .thenThrow(new RuntimeException());

      Optional<String> configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isEmpty());

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/db-password"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
    }
  }

  @Nested
  @DisplayName("Get env type tests")
  class GetEnvTypeTests {

    @Test
    @DisplayName("Correctly retrieves the environment type")
    public void getEnvType_testOk() {
      Value mockValue = ImmutableValue.builder()
        .key("carbonio-chats/chats-env")
        .value(Base64.getEncoder().encodeToString("dev".getBytes(StandardCharsets.UTF_8)))
        .createIndex(0L)
        .modifyIndex(0L)
        .lockIndex(0L)
        .flags(0L)
        .build();
      when(keyValueClient.getValue(eq("carbonio-chats/chats-env"), any(ImmutableQueryOptions.class)))
        .thenReturn(Optional.of(mockValue));

      EnvironmentType envType = appConfig.getEnvType();
      assertNotNull(envType);
      assertEquals(EnvironmentType.DEVELOPMENT, envType);

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/chats-env"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
    }

    @Test
    @DisplayName("Returns default env type if it was not retrieved")
    public void getEnvType_testNotRetrieved() {
      when(keyValueClient.getValue(eq("carbonio-chats/chats-env"), any(ImmutableQueryOptions.class)))
        .thenReturn(Optional.empty());

      EnvironmentType envType = appConfig.getEnvType();
      assertEquals(EnvironmentType.PRODUCTION, envType);

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/chats-env"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
    }

    @Test
    @DisplayName("Returns en empty optional if and exception is thrown")
    public void getEnvType_testException() {
      when(keyValueClient.getValue(eq("carbonio-chats/chats-env"), any(ImmutableQueryOptions.class)))
        .thenThrow(new RuntimeException());

      EnvironmentType envType = appConfig.getEnvType();
      assertEquals(EnvironmentType.PRODUCTION, envType);

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/chats-env"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
    }
  }
}