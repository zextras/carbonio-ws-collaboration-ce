package com.zextras.carbonio.chats.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class AppConfigTest {

  @Nested
  @DisplayName("Get attribute tests")
  class GetTest {

    @Test
    @DisplayName("Retrieves attribute from the first chain block")
    public void get_valueFromFirstBlock() {
      MockConfig config1 = new MockConfig().setConfig(ConfigName.DATABASE_JDBC_URL, "value1");
      MockConfig config2 = new MockConfig().setConfig(ConfigName.DATABASE_JDBC_URL, "value2");
      MockConfig config3 = new MockConfig().setConfig(ConfigName.DATABASE_JDBC_URL, "value3");

      config1.addToChain(config2).addToChain(config3);

      assertTrue(config1.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value1", config1.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the second chain block if the first is null")
    public void get_valueFromSecondBlock() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig().setConfig(ConfigName.DATABASE_JDBC_URL, "value2");
      MockConfig config3 = new MockConfig().setConfig(ConfigName.DATABASE_JDBC_URL, "value3");

      config1.addToChain(config2).addToChain(config3);

      assertTrue(config1.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value2", config1.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the third chain block if the second is null")
    public void get_valueFromThirdBlock() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig();
      MockConfig config3 = new MockConfig().setConfig(ConfigName.DATABASE_JDBC_URL, "value3");

      config1.addToChain(config2).addToChain(config3);

      assertTrue(config1.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value3", config1.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Returns and empty optional if no one solves the attribute")
    public void get_returnsEmptyOptional() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig();
      MockConfig config3 = new MockConfig();

      config1.addToChain(config2).addToChain(config3);

      assertFalse(config1.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
    }

  }

  @Nested
  @DisplayName("Get env type tests")
  class GetEnvTest {

    @Test
    @DisplayName("Retrieves env type from the first chain block")
    public void getEnvType_valueFromFirstBlock() {
      MockConfig config1 = new MockConfig().setEnv(EnvironmentType.DEVELOPMENT);
      MockConfig config2 = new MockConfig().setEnv(EnvironmentType.TEST);
      MockConfig config3 = new MockConfig().setEnv(EnvironmentType.TEST);

      config1.addToChain(config2).addToChain(config3);

      assertEquals(EnvironmentType.DEVELOPMENT, config1.getEnvType());
    }

    @Test
    @DisplayName("Retrieves env type from the second chain block if the first is null")
    public void getEnvType_valueFromSecondBlock() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig().setEnv(EnvironmentType.DEVELOPMENT);
      MockConfig config3 = new MockConfig().setEnv(EnvironmentType.TEST);

      config1.addToChain(config2).addToChain(config3);

      assertEquals(EnvironmentType.DEVELOPMENT, config1.getEnvType());
    }

    @Test
    @DisplayName("Retrieves env type from the third chain block if the second is null")
    public void getEnvType_valueFromThirdBlock() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig();
      MockConfig config3 = new MockConfig().setEnv(EnvironmentType.TEST);

      config1.addToChain(config2).addToChain(config3);

      assertEquals(EnvironmentType.TEST, config1.getEnvType());
    }

    @Test
    @DisplayName("Returns production env if no one solves the attribute")
    public void getEnvType_returnsDefaultEnv() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig();

      config1.addToChain(config2);

      assertEquals(EnvironmentType.PRODUCTION, config1.getEnvType());
    }

  }

  private static class MockConfig extends AppConfig {

    private final Map<ConfigName, String> configMap;
    private       EnvironmentType         environmentType;

    public MockConfig() {
      configMap = new HashMap<>();
      environmentType = null;
    }

    @Override
    protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
      return Optional.ofNullable(configMap.get(configName)).map(clazz::cast);
    }

    @Override
    protected Optional<EnvironmentType> getEnvTypeByImplementation() {
      return Optional.ofNullable(environmentType);
    }

    public MockConfig setConfig(ConfigName key, String value) {
      configMap.put(key, value);
      return this;
    }

    public MockConfig setEnv(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }
  }

}