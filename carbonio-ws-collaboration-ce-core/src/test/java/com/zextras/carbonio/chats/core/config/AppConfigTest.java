// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

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
        .add(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value2"))
        .add(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value3"));

      assertTrue(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value1", appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the second chain block if the first is null")
    public void get_valueFromSecondBlock() {
      AppConfig appConfig = MockAppConfig.create()
        .add(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value2"))
        .add(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value3"));

      assertTrue(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value2", appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Retrieves attribute from the third chain block if the second is null")
    public void get_valueFromThirdBlock() {
      AppConfig appConfig = MockAppConfig.create()
        .add(MockAppConfig.create())
        .add(MockAppConfig.create().set(ConfigName.DATABASE_JDBC_URL, "value3"));

      assertTrue(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
      assertEquals("value3", appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).get());
    }

    @Test
    @DisplayName("Returns and empty optional if no one solves the attribute")
    public void get_returnsEmptyOptional() {
      AppConfig appConfig = MockAppConfig.create()
        .add(MockAppConfig.create())
        .add(MockAppConfig.create());

      assertFalse(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).isPresent());
    }
  }
}