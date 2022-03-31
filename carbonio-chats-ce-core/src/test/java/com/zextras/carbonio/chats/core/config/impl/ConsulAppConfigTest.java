package com.zextras.carbonio.chats.core.config.impl;

import static org.mockito.Mockito.mock;

import com.ecwid.consul.v1.ConsulClient;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ConsulAppConfigTest {

  private ConsulAppConfig consulAppConfig;
  private ConsulClient    consulClient;

  public ConsulAppConfigTest() {
    consulClient = mock(ConsulClient.class);
    consulAppConfig = new ConsulAppConfig(consulClient);
  }

  @Disabled
  @Nested
  @DisplayName("Get attribute tests")
  class GetAttributesTests {

    @Test
    @DisplayName("Correctly retrieves an attribute")
    public void getAttribute_testOk() {
      consulAppConfig.get(String.class, ConfigValue.DATABASE_PASSWORD);
    }

  }

}