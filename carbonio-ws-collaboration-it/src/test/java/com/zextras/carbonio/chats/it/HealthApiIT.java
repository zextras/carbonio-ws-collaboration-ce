// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.HealthApi;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import com.zextras.carbonio.chats.it.tools.PreviewerMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.VideoServerMockServer;
import com.zextras.carbonio.chats.model.DependencyHealthDto;
import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;

@ApiIntegrationTest
public class HealthApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final PreviewerMockServer       previewerMockServer;
  private final MongooseImMockServer      mongooseImMockServer;
  private final VideoServerMockServer     videoServerMockServer;

  public HealthApiIT(
    HealthApi healthApi,
    ResteasyRequestDispatcher dispatcher,
    ObjectMapper objectMapper,
    PreviewerMockServer previewerMockServer,
    MongooseImMockServer mongooseImMockServer,
    VideoServerMockServer videoServerMockServer
  ) {
    this.dispatcher = dispatcher;
    this.previewerMockServer = previewerMockServer;
    this.mongooseImMockServer = mongooseImMockServer;
    this.videoServerMockServer = videoServerMockServer;
    this.dispatcher.getRegistry().addSingletonResource(healthApi);
    this.objectMapper = objectMapper;
  }

  @AfterEach
  public void afterEach() {
    mongooseImMockServer.mockIsAlive(true);
    previewerMockServer.setIsAliveResponse(true);
  }

  @Nested
  @DisplayName("Gets health status tests")
  public class GetsHealthStatusTests {

    @Test
    @DisplayName("Returns the success health status")
    public void getHealthStatus_TestOk() throws Exception {
      mongooseImMockServer.mockIsAlive(true);
      videoServerMockServer.mockRequestedResponse("GET", "/janus/info",
        "{\"janus\":\"server_info\"}", true);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus = objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.OK, healthStatus.getStatus());
      assertEquals(0, (int) healthStatus.getDependencies().stream().map(DependencyHealthDto::isIsHealthy)
        .filter(isLive -> !isLive).count());
      mongooseImMockServer.verify(mongooseImMockServer.getIsAliveRequest(), VerificationTimes.exactly(2));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("GET", "/janus/info"), VerificationTimes.exactly(2));
    }

    @Test
    @DisplayName("Returns the health status when previewer isn't alive")
    public void getHealthStatus_TestWarn() throws Exception {
      mongooseImMockServer.mockIsAlive(true);
      videoServerMockServer.mockRequestedResponse("GET", "/janus/info",
        "{\"janus\":\"server_info\"}", true);
      previewerMockServer.setIsAliveResponse(false);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus = objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.WARN, healthStatus.getStatus());

      List<DependencyHealthDto> failedDependencies = healthStatus.getDependencies().stream()
        .filter(dependency -> !dependency.isIsHealthy()).collect(Collectors.toList());
      assertEquals(1, failedDependencies.size());
      assertEquals(DependencyHealthTypeDto.PREVIEWER_SERVICE, failedDependencies.get(0).getName());
      mongooseImMockServer.verify(mongooseImMockServer.getIsAliveRequest(), VerificationTimes.exactly(2));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("GET", "/janus/info"), VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Returns the health status when xmpp server isn't alive")
    public void getHealthStatus_TestError() throws Exception {
      videoServerMockServer.mockRequestedResponse("GET", "/janus/info",
        "{\"janus\":\"server_info\"}", true);
      mongooseImMockServer.mockIsAlive(false);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus = objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.ERROR, healthStatus.getStatus());

      List<DependencyHealthDto> failedDependencies = healthStatus.getDependencies().stream()
        .filter(dependency -> !dependency.isIsHealthy()).collect(Collectors.toList());
      assertEquals(1, failedDependencies.size());
      assertEquals(DependencyHealthTypeDto.XMPP_SERVER, failedDependencies.get(0).getName());
      mongooseImMockServer.verify(mongooseImMockServer.getIsAliveRequest(), VerificationTimes.exactly(2));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("GET", "/janus/info"), VerificationTimes.exactly(1));
    }
  }

  @Nested
  @DisplayName("Checks live tests")
  public class ChecksLiveTests {

    @Test
    @DisplayName("Checks correct live test")
    public void isLive_testOk() throws Exception {
      MockHttpResponse response = dispatcher.get("/health/live");
      assertEquals(204, response.getStatus());
    }

  }


  @Nested
  @DisplayName("Checks ready tests")
  public class ChecksReadyTests {

    @Test
    @DisplayName("Checks correct ready test")
    public void isReady_testOk() throws Exception {
      mongooseImMockServer.mockIsAlive(true);
      videoServerMockServer.mockRequestedResponse("GET", "/janus/info",
        "{\"janus\":\"server_info\"}", true);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(204, response.getStatus());
      mongooseImMockServer.verify(mongooseImMockServer.getIsAliveRequest(), VerificationTimes.exactly(1));
      videoServerMockServer.verify(
        videoServerMockServer.getRequest("GET", "/janus/info"), VerificationTimes.exactly(1));
    }


    @Test
    @DisplayName("Checks warn ready test")
    public void isReady_testWarn() throws Exception {
      mongooseImMockServer.mockIsAlive(true);
      previewerMockServer.setIsAliveResponse(false);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(204, response.getStatus());
      mongooseImMockServer.verify(mongooseImMockServer.getIsAliveRequest(), VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Checks error ready test")
    public void isReady_testError() throws Exception {
      mongooseImMockServer.mockIsAlive(false);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(500, response.getStatus());
      mongooseImMockServer.verify(mongooseImMockServer.getIsAliveRequest(), VerificationTimes.exactly(1));
    }
  }
}
