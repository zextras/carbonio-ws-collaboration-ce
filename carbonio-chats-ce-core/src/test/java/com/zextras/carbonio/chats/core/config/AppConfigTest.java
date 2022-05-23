package com.zextras.carbonio.chats.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.impl.MockAppConfig;
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
      AppConfig appConfig = MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value1")
        .addToChain(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value2"))
        .addToChain(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value3"));

      assertTrue(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value1", appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the second chain block if the first is null")
    public void get_valueFromSecondBlock() {
      AppConfig appConfig = MockAppConfig.create()
        .addToChain(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value2"))
        .addToChain(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value3"));

      assertTrue(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value2", appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the third chain block if the second is null")
    public void get_valueFromThirdBlock() {
      AppConfig appConfig = MockAppConfig.create()
        .addToChain(MockAppConfig.create())
        .addToChain(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value3"));

      assertTrue(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value3", appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Returns and empty optional if no one solves the attribute")
    public void get_returnsEmptyOptional() {
      AppConfig appConfig = MockAppConfig.create()
        .addToChain(MockAppConfig.create())
        .addToChain(MockAppConfig.create());

      assertFalse(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
    }
  }

  @Nested
  @DisplayName("Get env type tests")
  class GetEnvTest {

    @Test
    @DisplayName("Retrieves env type from the first chain block")
    public void getEnvType_valueFromFirstBlock() {
      AppConfig appConfig = MockAppConfig.create().set(ConfigName.ENV, EnvironmentType.DEVELOPMENT.getName())
        .addToChain(MockAppConfig.create().set(ConfigName.ENV, EnvironmentType.TEST.getName()))
        .addToChain(MockAppConfig.create().set(ConfigName.ENV, EnvironmentType.TEST.getName()));

      assertEquals(EnvironmentType.DEVELOPMENT, appConfig.getEnvType());
    }

    @Test
    @DisplayName("Retrieves env type from the second chain block if the first is null")
    public void getEnvType_valueFromSecondBlock() {
      AppConfig appConfig = MockAppConfig.create()
        .addToChain(MockAppConfig.create().set(ConfigName.ENV, EnvironmentType.DEVELOPMENT.getName()))
        .addToChain(MockAppConfig.create().set(ConfigName.ENV, EnvironmentType.TEST.getName()));

      assertEquals(EnvironmentType.DEVELOPMENT, appConfig.getEnvType());
    }

    @Test
    @DisplayName("Retrieves env type from the third chain block if the second is null")
    public void getEnvType_valueFromThirdBlock() {
      AppConfig appConfig = MockAppConfig.create()
        .addToChain(MockAppConfig.create())
        .addToChain(MockAppConfig.create().set(ConfigName.ENV, EnvironmentType.TEST.getName()));

      assertEquals(EnvironmentType.TEST, appConfig.getEnvType());
    }

    @Test
    @DisplayName("Returns production env if no one solves the attribute")
    public void getEnvType_returnsDefaultEnv() {
      AppConfig appConfig = MockAppConfig.create()
        .addToChain(MockAppConfig.create());

      assertEquals(EnvironmentType.PRODUCTION, appConfig.getEnvType());
    }
  }
}