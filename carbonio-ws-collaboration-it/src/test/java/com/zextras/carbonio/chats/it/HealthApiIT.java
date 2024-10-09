// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.HealthApi;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.*;
import com.zextras.carbonio.chats.model.DependencyHealthDto;
import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;
import java.util.List;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class HealthApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final StorageMockServer storageMockServer;
  private final MongooseImMockServer mongooseImMockServer;
  private final PreviewerMockServer previewerMockServer;
  private final VideoServerMockServer videoServerMockServer;

  public HealthApiIT(
      HealthApi healthApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      UserManagementMockServer userManagementMockServer,
      StorageMockServer storageMockServer,
      MongooseImMockServer mongooseImMockServer,
      PreviewerMockServer previewerMockServer,
      VideoServerMockServer videoServerMockServer) {
    this.dispatcher = dispatcher;
    this.storageMockServer = storageMockServer;
    this.mongooseImMockServer = mongooseImMockServer;
    this.previewerMockServer = previewerMockServer;
    this.videoServerMockServer = videoServerMockServer;
    this.dispatcher.getRegistry().addSingletonResource(healthApi);
    this.objectMapper = objectMapper;
  }

  @AfterEach
  public void afterEach() {
    storageMockServer.setIsAliveResponse(true);
    mongooseImMockServer.setIsAliveResponse(true);
    previewerMockServer.setIsAliveResponse(true);
  }

  @Nested
  @DisplayName("Gets health status tests")
  class GetsHealthStatusTests {

    @Test
    @DisplayName("Returns the success health status")
    void getHealthStatus_TestOk() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus =
          objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.OK, healthStatus.getStatus());
      assertEquals(
          0,
          (int)
              healthStatus.getDependencies().stream()
                  .map(DependencyHealthDto::isIsHealthy)
                  .filter(isLive -> !isLive)
                  .count());
    }

    @Test
    @DisplayName("Returns the health status when storage isn't alive")
    void getHealthStatus_TestWarnStorage() throws Exception {
      storageMockServer.setIsAliveResponse(false);
      mongooseImMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus =
          objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.WARN, healthStatus.getStatus());

      List<DependencyHealthDto> failedDependencies =
          healthStatus.getDependencies().stream()
              .filter(dependency -> !dependency.isIsHealthy())
              .toList();
      assertEquals(1, failedDependencies.size());
      assertEquals(DependencyHealthTypeDto.STORAGE_SERVICE, failedDependencies.get(0).getName());
    }

    @Test
    @DisplayName("Returns the health status when xmpp server isn't alive")
    void getHealthStatus_TestErrorMongoose() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(false);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus =
          objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.ERROR, healthStatus.getStatus());

      List<DependencyHealthDto> failedDependencies =
          healthStatus.getDependencies().stream()
              .filter(dependency -> !dependency.isIsHealthy())
              .toList();
      assertEquals(1, failedDependencies.size());
      assertEquals(DependencyHealthTypeDto.XMPP_SERVER, failedDependencies.get(0).getName());
    }

    @Test
    @DisplayName("Returns the health status when videoserver isn't alive")
    void getHealthStatus_TestWarnVideoserver() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(false);
      previewerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus =
          objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.WARN, healthStatus.getStatus());

      List<DependencyHealthDto> failedDependencies =
          healthStatus.getDependencies().stream()
              .filter(dependency -> !dependency.isIsHealthy())
              .toList();
      assertEquals(1, failedDependencies.size());
      assertEquals(
          DependencyHealthTypeDto.VIDEOSERVER_SERVICE, failedDependencies.get(0).getName());
    }

    @Test
    @DisplayName("Returns the health status when previewer isn't alive")
    void getHealthStatus_TestWarnPreviewer() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(false);
      MockHttpResponse response = dispatcher.get("/health");
      assertEquals(200, response.getStatus());
      HealthStatusDto healthStatus =
          objectMapper.readValue(response.getContentAsString(), HealthStatusDto.class);
      assertEquals(DependencyHealthTypeDto.values().length, healthStatus.getDependencies().size());
      assertEquals(HealthStatusTypeDto.WARN, healthStatus.getStatus());

      List<DependencyHealthDto> failedDependencies =
          healthStatus.getDependencies().stream()
              .filter(dependency -> !dependency.isIsHealthy())
              .toList();
      assertEquals(1, failedDependencies.size());
      assertEquals(DependencyHealthTypeDto.PREVIEWER_SERVICE, failedDependencies.get(0).getName());
    }
  }

  @Nested
  @DisplayName("Checks live tests")
  class ChecksLiveTests {

    @Test
    @DisplayName("Checks correct live test")
    void isLive_testOk() throws Exception {
      MockHttpResponse response = dispatcher.get("/health/live");
      assertEquals(204, response.getStatus());
    }
  }

  @Nested
  @DisplayName("Checks ready tests")
  class ChecksReadyTests {

    @Test
    @DisplayName("Checks correct ready test")
    void isReady_testOk() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(204, response.getStatus());
    }

    @Test
    @DisplayName("Checks warn ready test when storage is not alive")
    void isReady_testWarnStorage() throws Exception {
      storageMockServer.setIsAliveResponse(false);
      mongooseImMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(204, response.getStatus());
    }

    @Test
    @DisplayName("Checks error ready test when xmpp server isn't alive")
    void isReady_testErrorMongoose() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(false);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(424, response.getStatus());
    }

    @Test
    @DisplayName("Checks warn ready test when previewer isn't alive")
    void isReady_testWarnPreviewer() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(false);
      videoServerMockServer.setIsAliveResponse(true);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(204, response.getStatus());
    }

    @Test
    @DisplayName("Checks warn ready test when videoserver isn't alive")
    void isReady_testWarnVideoserver() throws Exception {
      storageMockServer.setIsAliveResponse(true);
      mongooseImMockServer.setIsAliveResponse(true);
      previewerMockServer.setIsAliveResponse(true);
      videoServerMockServer.setIsAliveResponse(false);
      MockHttpResponse response = dispatcher.get("/health/ready");
      assertEquals(204, response.getStatus());
    }
  }
}
