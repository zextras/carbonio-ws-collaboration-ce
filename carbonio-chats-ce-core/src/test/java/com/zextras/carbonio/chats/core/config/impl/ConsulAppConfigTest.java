package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ConsulAppConfigTest {

  private ConsulAppConfig consulAppConfig;
  private ConsulClient    consulClient;

  public ConsulAppConfigTest() {
    consulClient = mock(ConsulClient.class);
    consulAppConfig = new ConsulAppConfig(consulClient, "TOKEN");
  }

  @Nested
  @DisplayName("Get attribute tests")
  class GetAttributesTests {

    @Test
    @DisplayName("Correctly retrieves an attribute")
    public void getAttribute_testOk() {
      GetValue mockValue = mock(GetValue.class);
      when(mockValue.getDecodedValue()).thenReturn("testpsw");
      when(consulClient.getKVValue(ConfigValue.DATABASE_PASSWORD.getConsulName(), "TOKEN"))
        .thenReturn(new Response<>(mockValue, 12L, true, 12L));

      Optional<String> configValue = consulAppConfig.getConfigByImplementation(String.class,
        ConfigValue.DATABASE_PASSWORD);

      assertTrue(configValue.isPresent());
      assertEquals("testpsw", configValue.get());
    }

    @Test
    @DisplayName("Returns en empty optional if the attribute was not retrieved")
    public void getAttribute_testNotRetrieved() {
      GetValue mockValue = mock(GetValue.class);
      when(mockValue.getDecodedValue()).thenReturn(null);
      when(consulClient.getKVValue(ConfigValue.DATABASE_PASSWORD.getConsulName(), "TOKEN"))
        .thenReturn(new Response<>(mockValue, 12L, true, 12L));

      Optional<String> configValue = consulAppConfig.getConfigByImplementation(String.class,
        ConfigValue.DATABASE_PASSWORD);

      assertTrue(configValue.isEmpty());
    }

    @Test
    @DisplayName("Returns en empty optional if and exception is thrown")
    public void getAttribute_testException() {
      when(consulClient.getKVValue(ConfigValue.DATABASE_PASSWORD.getConsulName(), "TOKEN"))
        .thenThrow(new RuntimeException());

      Optional<String> configValue = consulAppConfig.getConfigByImplementation(String.class,
        ConfigValue.DATABASE_PASSWORD);

      assertTrue(configValue.isEmpty());
    }

  }

  @Nested
  @DisplayName("Get env type tests")
  class GetEnvTypeTests {

    @Test
    @DisplayName("Correctly retrieves the environment type")
    public void getEnvType_testOk() {
      GetValue mockValue = mock(GetValue.class);
      when(mockValue.getDecodedValue()).thenReturn("dev");
      when(consulClient.getKVValue(ConfigValue.ENV.getConsulName(), "TOKEN"))
        .thenReturn(new Response<>(mockValue, 12L, true, 12L));

      Optional<EnvironmentType> envType = consulAppConfig.getEnvTypeByImplementation();

      assertTrue(envType.isPresent());
      assertEquals(EnvironmentType.DEVELOPMENT, envType.get());
    }

    @Test
    @DisplayName("Returns en empty optional if the env type was not retrieved")
    public void getEnvType_testNotRetrieved() {
      GetValue mockValue = mock(GetValue.class);
      when(mockValue.getDecodedValue()).thenReturn(null);
      when(consulClient.getKVValue(ConfigValue.ENV.getConsulName(), "TOKEN"))
        .thenReturn(new Response<>(mockValue, 12L, true, 12L));

      Optional<EnvironmentType> envType = consulAppConfig.getEnvTypeByImplementation();

      assertTrue(envType.isEmpty());
    }

    @Test
    @DisplayName("Returns en empty optional if and exception is thrown")
    public void getEnvType_testException() {
      when(consulClient.getKVValue(ConfigValue.ENV.getConsulName(), "TOKEN")).thenThrow(new RuntimeException());

      Optional<EnvironmentType> envType = consulAppConfig.getEnvTypeByImplementation();

      assertTrue(envType.isEmpty());

    }

  }

}