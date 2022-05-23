package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.TestConsulAppConfig;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class ConsulApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final AppConfig                 appConfig;

  public ConsulApiIT(
    ResteasyRequestDispatcher dispatcher, AppConfig appConfig
  ) {
    this.dispatcher = dispatcher;
    this.appConfig = appConfig;
  }

  @Nested
  @DisplayName("Sets the Consul properties tests")
  class SetConsulPropertiesChangedBySuffixTests {

    private static final String URL = "/consul/kv-store";

    @Test
    @DisplayName("Correctly sets a new value of an existing Consul properties configuration")
    public void setExistingConsulProperties_testOK() throws Exception {
      appConfig.addToChain(TestConsulAppConfig.create()
        .set(ConfigName.HIKARI_IDLE_TIMEOUT, "hikariIdleTimeout"));

      String requestBody = getConsulPropertyBody(Map.of("carbonio-chats/hikari-idle-timeout", "500"));
      MockHttpResponse mockHttpResponse = dispatcher.post(URL, requestBody, null);
      assertEquals(204, mockHttpResponse.getStatus());

      Optional<String> next = appConfig.get(String.class, ConfigName.HIKARI_IDLE_TIMEOUT);
      assertTrue(next.isPresent());
      assertEquals("500", next.get());
      appConfig.removeLast();
    }

    @Test
    @DisplayName("Correctly sets a new  Consul properties configuration")
    public void setNewConsulProperties_testOK() throws Exception {
      appConfig.addToChain(TestConsulAppConfig.create());
//        .set(ConfigName.HIKARI_IDLE_TIMEOUT, "hikariIdleTimeout"));

      String requestBody = getConsulPropertyBody(Map.of("carbonio-chats/hikari-leak-detection-threshold", "10"));
      MockHttpResponse mockHttpResponse = dispatcher.post(URL, requestBody, null);

      assertEquals(204, mockHttpResponse.getStatus());
      Optional<String> next = appConfig.get(String.class, ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD);
      assertTrue(next.isPresent());
      assertEquals("10", next.get());
      appConfig.removeLast();
    }

    private String getConsulPropertyBody(Map<String, String> keyValueMap) {
      StringBuilder sb = new StringBuilder();
      keyValueMap.forEach((k, v) ->
        sb.append(sb.length() == 0 ? "" : ",").append(String.format(
          "{\"key\": \"%s\",\"createIndex\": 0,\"modifyIndex\": 0, \"lockIndex\": 0,\"flags\": 0,\"value\": \"%s\",\"session\": \"\"}",
          k, Base64.getEncoder().encodeToString(v.getBytes()))));
      return "[" + sb + "]";
    }
  }

}
