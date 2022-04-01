package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.config.ConfigProvider;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConfigIT {

  private ConfigProvider configProvider;

  @TempDir
  Path tempConfigPath;

  @BeforeEach
  public void injectorSetup() {
    configProvider = new ConfigProvider(tempConfigPath, tempConfigPath.resolve("config.properties"));
  }

  @Test
  @DisplayName("Correctly solves the configuration with environment first")
  public void envConfigurationFirstTest() throws Exception {
    Path envFile = tempConfigPath.resolve(".env");
    Path propertiesFile = tempConfigPath.resolve("config.properties");
    Files.writeString(envFile, String.format("%s=%s", ConfigValue.DATABASE_PASSWORD.getEnvName(), "envpsw"));
    Files.writeString(propertiesFile,
      String.format("%s=%s", ConfigValue.DATABASE_PASSWORD.getPropertyName(), "proppsw"));

    Optional<String> paramValue = configProvider.get().get(String.class, ConfigValue.DATABASE_PASSWORD);

    assertTrue(paramValue.isPresent());
    assertEquals("envpsw", paramValue.get());
  }

  @Test
  @DisplayName("Correctly solves the configuration with properties if environment is not set")
  public void propConfigurationTest() throws Exception {
    Path propertiesFile = tempConfigPath.resolve("config.properties");
    Files.writeString(propertiesFile,
      String.format("%s=%s", ConfigValue.DATABASE_PASSWORD.getPropertyName(), "proppsw"));

    Optional<String> paramValue = configProvider.get().get(String.class, ConfigValue.DATABASE_PASSWORD);

    assertTrue(paramValue.isPresent());
    assertEquals("proppsw", paramValue.get());
  }

  @Test
  @DisplayName("Returns an empty optional if nothing is set")
  public void emptyConfigurationTest() {
    Optional<String> paramValue = configProvider.get().get(String.class, ConfigValue.DATABASE_PASSWORD);

    assertTrue(paramValue.isEmpty());
  }
}
