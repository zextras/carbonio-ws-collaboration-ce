package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.HealthApi;
import com.zextras.carbonio.chats.it.annotations.IntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class HealthApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;

  public HealthApiIT(HealthApi healthApi, ResteasyRequestDispatcher dispatcher, ObjectMapper objectMapper) {
    this.dispatcher = dispatcher;
    this.dispatcher.getRegistry().addSingletonResource(healthApi);
    this.objectMapper = objectMapper;
  }

  @Test
  @Disabled
  public void getHealthStatusTest() throws Exception {
    MockHttpResponse response = dispatcher.get("/health");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    String contentAsString = response.getContentAsString();
    HealthStatusDto healthStatus = objectMapper.readValue(contentAsString, HealthStatusDto.class);
    assertEquals(6, healthStatus.getDependencies().size());
  }

}
