package com.zextras.carbonio.chats.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@UnitTest
class ConfigFactoryTest {

  private ConfigFactory configFactory;
  private ConsulClient  consulClient;
  @TempDir
  Path tempConfigPath;

  @BeforeEach
  public void injectorSetup() {
    consulClient = mock(ConsulClient.class);
    configFactory = new ConfigFactory(tempConfigPath, tempConfigPath.resolve("config.properties"), consulClient);
  }

  @Test
  @DisplayName("Correctly solves the configuration with environment first")
  public void envConfigurationFirstTest() throws Exception {
    Path envFile = tempConfigPath.resolve(".env");
    Path propertiesFile = tempConfigPath.resolve("config.properties");

    Files.write(envFile, List.of(
      String.format("%s=%s", ConfigName.DATABASE_PASSWORD.getEnvName(), "envpsw"),
      String.format("%s=%s", ConfigName.CONSUL_TOKEN.getEnvName(), "token") //mock it as an env variable
    ));
    Files.writeString(propertiesFile,
      String.format("%s=%s", ConfigName.DATABASE_PASSWORD.getPropertyName(), "proppsw"));
    GetValue mockValue = mock(GetValue.class);
    when(mockValue.getDecodedValue()).thenReturn("consulpsw");
    when(consulClient.getKVValue(ConfigName.DATABASE_PASSWORD.getConsulName(), "token"))
      .thenReturn(new Response<>(mockValue, 12L, true, 12L));

    Optional<String> paramValue = configFactory.create().get(String.class, ConfigName.DATABASE_PASSWORD);

    assertTrue(paramValue.isPresent());
    assertEquals("envpsw", paramValue.get());
  }

  @Test
  @DisplayName("Correctly solves the configuration with properties if environment is not set")
  public void propConfigurationTest() throws Exception {
    Path envFile = tempConfigPath.resolve(".env");
    Path propertiesFile = tempConfigPath.resolve("config.properties");

    Files.write(envFile, List.of(
      String.format("%s=%s", ConfigName.CONSUL_TOKEN.getEnvName(), "token") //mock it as an env variable
    ));
    Files.writeString(propertiesFile,
      String.format("%s=%s", ConfigName.DATABASE_PASSWORD.getPropertyName(), "proppsw"));
    GetValue mockValue = mock(GetValue.class);
    when(mockValue.getDecodedValue()).thenReturn("consulpsw");
    when(consulClient.getKVValue(ConfigName.DATABASE_PASSWORD.getConsulName(), "token"))
      .thenReturn(new Response<>(mockValue, 12L, true, 12L));

    Optional<String> paramValue = configFactory.create().get(String.class, ConfigName.DATABASE_PASSWORD);

    assertTrue(paramValue.isPresent());
    assertEquals("proppsw", paramValue.get());
  }

  @Test
  @DisplayName("Correctly solves the configuration with consul if properties and environment are not set")
  public void consulConfigurationTest() throws Exception {
    Path envFile = tempConfigPath.resolve(".env");

    Files.write(envFile, List.of(
      String.format("%s=%s", ConfigName.CONSUL_TOKEN.getEnvName(), "token") //mock it as an env variable
    ));
    GetValue mockValue = mock(GetValue.class);
    when(mockValue.getKey()).thenReturn("carbonio-chats/db-password");
    when(mockValue.getDecodedValue()).thenReturn("consulpsw");
    when(consulClient.getKVValues("carbonio-chats", "token"))
      .thenReturn(new Response<>(List.of(mockValue), 12L, true, 12L));

    Optional<String> paramValue = configFactory.create().get(String.class, ConfigName.DATABASE_PASSWORD);

    assertTrue(paramValue.isPresent());
    assertEquals("consulpsw", paramValue.get());
  }

  @Test
  @DisplayName("Returns an empty optional if nothing is set")
  public void emptyConfigurationTest() throws Exception {
    Path envFile = tempConfigPath.resolve(".env");

    Files.write(envFile, List.of(
      String.format("%s=%s", ConfigName.CONSUL_TOKEN.getEnvName(), "token") //mock it as an env variable
    ));

    Optional<String> paramValue = configFactory.create().get(String.class, ConfigName.DATABASE_PASSWORD);

    assertTrue(paramValue.isEmpty());
  }

  @Test
  @DisplayName("Returns an empty optional if consul token is not set")
  public void consulTokenNotSetTest() {
    GetValue mockValue = mock(GetValue.class);
    when(mockValue.getDecodedValue()).thenReturn("consulpsw");
    when(consulClient.getKVValue(ConfigName.DATABASE_PASSWORD.getConsulName(), "token"))
      .thenReturn(new Response<>(mockValue, 12L, true, 12L));

    Optional<String> paramValue = configFactory.create().get(String.class, ConfigName.DATABASE_PASSWORD);

    assertTrue(paramValue.isEmpty());
  }
}