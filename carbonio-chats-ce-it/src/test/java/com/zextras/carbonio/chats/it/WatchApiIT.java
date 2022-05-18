package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ConsulMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class WatchApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final AppConfig                 appConfig;
  private final ConsulMockServer          consulMockServer;

  public WatchApiIT(
    ResteasyRequestDispatcher dispatcher, AppConfig appConfig,
    ConsulMockServer consulMockServer
  ) {
    this.dispatcher = dispatcher;
    this.appConfig = appConfig;
    this.consulMockServer = consulMockServer;
  }

  @AfterEach
  public void afterEach() {
  }

  @Nested
  @DisplayName("Sets the Consul properties tests")
  class SetConsulPropertiesChangedBySuffix {

    private static final String URL = "/watch/consul/prefix";

    @Test
    @DisplayName("Correctly sets a new value of an existing Consul properties configuration")
    public void setExistingConsulProperties_testOK() throws Exception {
      Optional<String> prev = appConfig.get(String.class, ConfigName.HIKARI_IDLE_TIMEOUT);
      assertTrue(prev.isPresent());
      assertEquals("hikariIdleTimeout", prev.get());

      String requestBody = getConsulPropertyBody(Map.of("carbonio-chats/hikari-idle-timeout", "500"));
      MockHttpResponse mockHttpResponse = dispatcher.post(URL, requestBody, null);
      assertEquals(204, mockHttpResponse.getStatus());

      Optional<String> next = appConfig.get(String.class, ConfigName.HIKARI_IDLE_TIMEOUT);
      assertTrue(next.isPresent());
      assertNotEquals(prev, next);
      assertEquals("500", next.get());
    }

    @Test
    @DisplayName("Correctly sets a new  Consul properties configuration")
    public void setNewConsulProperties_testOK() throws Exception {
      Optional<Integer> prev = appConfig.get(Integer.class, ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD);
      assertFalse(prev.isPresent());

      String requestBody = getConsulPropertyBody(Map.of("carbonio-chats/hikari-leak-detection-threshold", "10"));
      MockHttpResponse mockHttpResponse = dispatcher.post(URL, requestBody, null);

      assertEquals(204, mockHttpResponse.getStatus());
      Optional<String> next = appConfig.get(String.class, ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD);
      assertTrue(next.isPresent());
      assertEquals("10", next.get());
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
