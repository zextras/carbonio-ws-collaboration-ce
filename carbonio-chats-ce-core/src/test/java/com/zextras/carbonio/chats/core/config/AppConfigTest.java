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
    public void get_retrieFromFirstBlock() {
      MockConfig config1 = new MockConfig().setConfig(ConfigValue.DATABASE_JDBC_URL, "value1");
      MockConfig config2 = new MockConfig().setConfig(ConfigValue.DATABASE_JDBC_URL, "value2");

      config1.or(config2);

      assertTrue(config1.get(String.class, ConfigValue.DATABASE_JDBC_URL).isPresent());
      assertEquals("value1", config1.get(String.class, ConfigValue.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the second chain block if the first is null")
    public void get_retrieFromSecondBlock() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig().setConfig(ConfigValue.DATABASE_JDBC_URL, "value2");

      config1.or(config2);

      assertTrue(config1.get(String.class, ConfigValue.DATABASE_JDBC_URL).isPresent());
      assertEquals("value2", config1.get(String.class, ConfigValue.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Returns and empty optional if no one solves the attribute")
    public void get_returnsEmptyOptional() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig();

      config1.or(config2);

      assertFalse(config1.get(String.class, ConfigValue.DATABASE_JDBC_URL).isPresent());
    }

  }

  @Nested
  @DisplayName("Get env type tests")
  class GetEnvTest {

    @Test
    @DisplayName("Retrieves env type from the first chain block")
    public void getEnvType_retrieFromFirstBlock() {
      MockConfig config1 = new MockConfig().setEnv(EnvironmentType.DEVELOPMENT);
      MockConfig config2 = new MockConfig().setEnv(EnvironmentType.TEST);

      config1.or(config2);

      assertEquals(EnvironmentType.DEVELOPMENT, config1.getEnvType());
    }

    @Test
    @DisplayName("Retrieves env type from the second chain block if the first is null")
    public void getEnvType_retrieFromSecondBlock() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig().setEnv(EnvironmentType.TEST);

      config1.or(config2);

      assertEquals(EnvironmentType.TEST, config1.getEnvType());
    }

    @Test
    @DisplayName("Returns production env if no one solves the attribute")
    public void getEnvType_returnsEmptyOptional() {
      MockConfig config1 = new MockConfig();
      MockConfig config2 = new MockConfig();

      config1.or(config2);

      assertEquals(EnvironmentType.PRODUCTION, config1.getEnvType());
    }

  }

  private static class MockConfig extends AppConfig {

    private final Map<ConfigValue, String> configMap;
    private       EnvironmentType          environmentType;

    public MockConfig() {
      configMap = new HashMap<>();
      environmentType = null;
    }

    @Override
    protected <T> Optional<T> getAttributeByImplementation(Class<T> clazz, ConfigValue configName) {
      return Optional.ofNullable(configMap.get(configName)).map(clazz::cast);
    }

    @Override
    protected Optional<EnvironmentType> getEnvTypeByImplementation() {
      return Optional.ofNullable(environmentType);
    }

    public MockConfig setConfig(ConfigValue key, String value) {
      configMap.put(key, value);
      return this;
    }

    public MockConfig setEnv(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }
  }

}