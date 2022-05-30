package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.ImmutableValue;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ConsulAppConfigTest {

  private final AppConfig      appConfig;
  private final Consul         consulClient;
  private final KeyValueClient keyValueClient;

  public ConsulAppConfigTest() {
    consulClient = mock(Consul.class);
    keyValueClient = mock(KeyValueClient.class);
    when(consulClient.keyValueClient()).thenReturn(keyValueClient);
    appConfig = Optional.of(ConsulAppConfig.create(consulClient, "TOKEN")).orElseThrow();
  }

  @Nested
  @DisplayName("Load required configurations tests")
  class LoadRequiredConfigurationsTests {

    @Test
    @DisplayName("Correctly retrieves required configurations and cache them")
    public void loadConfigurations_testOk() {
      Value mockValue1 = mock(ImmutableValue.class);
      when(mockValue1.getKey()).thenReturn("carbonio-chats/db-username");
      when(mockValue1.getValue()).thenReturn(Optional.of("dbUsername"));
      Value mockValue2 = mock(ImmutableValue.class);
      when(mockValue2.getKey()).thenReturn("carbonio-chats/db-password");
      when(mockValue2.getValue()).thenReturn(Optional.of("dbPassword"));
      when(keyValueClient.getValues(eq("carbonio-chats/"), any(QueryOptions.class)))
        .thenReturn(List.of(mockValue1, mockValue2));

      appConfig.load();
      Optional<String> configValue;
      configValue = appConfig.get(String.class, ConfigName.DATABASE_USERNAME);
      assertTrue(configValue.isPresent());
      assertEquals("dbUsername", configValue.get());
      configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isPresent());
      assertEquals("dbPassword", configValue.get());

      verify(keyValueClient, times(1))
        .getValues(eq("carbonio-chats/"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
    }
  }

  @Nested
  @DisplayName("Get attribute tests")
  class GetAttributesTests {

    @Test
    @DisplayName("Correctly retrieves an attribute")
    public void getAttribute_testOk() {
      Value mockValue = mock(ImmutableValue.class);
      when(mockValue.getKey()).thenReturn("carbonio-chats/db-password");
      when(mockValue.getValueAsString()).thenReturn(Optional.of("testpsw"));
      when(keyValueClient.getValue(eq("carbonio-chats/db-password"), any(QueryOptions.class)))
        .thenReturn(Optional.of(mockValue));

      Optional<String> configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isPresent());
      assertEquals("testpsw", configValue.get());

      configValue = appConfig.get(String.class, ConfigName.DATABASE_PASSWORD);
      assertTrue(configValue.isPresent());
      assertEquals("testpsw", configValue.get());

      verify(keyValueClient, times(1))
        .getValue(eq("carbonio-chats/db-password"), any(ImmutableQueryOptions.class));
      verify(consulClient, times(1)).keyValueClient();
      verifyNoMoreInteractions(keyValueClient, consulClient);
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
      Value mockValue = mock(ImmutableValue.class);
      when(mockValue.getKey()).thenReturn("carbonio-chats/chats-env");
      when(mockValue.getValueAsString()).thenReturn(Optional.of("dev"));
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