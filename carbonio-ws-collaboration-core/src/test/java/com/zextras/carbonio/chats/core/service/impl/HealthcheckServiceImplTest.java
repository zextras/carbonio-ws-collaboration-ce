// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.preview.PreviewService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.model.DependencyHealthDto;
import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class HealthcheckServiceImplTest {

  private final HealthcheckServiceImpl healthcheckService;
  private final MessageDispatcher messageDispatcher;
  private final DatabaseInfoService databaseInfoService;
  private final EventDispatcher eventDispatcher;
  private final StoragesService storagesService;
  private final PreviewService previewService;
  private final AuthenticationService authenticationService;
  private final ProfilingService profilingService;
  private final VideoServerService videoServerService;

  public HealthcheckServiceImplTest() {
    this.messageDispatcher = mock(MessageDispatcher.class);
    this.databaseInfoService = mock(DatabaseInfoService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.storagesService = mock(StoragesService.class);
    this.previewService = mock(PreviewService.class);
    this.authenticationService = mock(AuthenticationService.class);
    this.profilingService = mock(ProfilingService.class);
    this.videoServerService = mock(VideoServerService.class);
    this.healthcheckService =
        new HealthcheckServiceImpl(
            this.messageDispatcher,
            this.databaseInfoService,
            this.eventDispatcher,
            this.storagesService,
            this.previewService,
            this.authenticationService,
            this.profilingService,
            this.videoServerService);
  }

  @AfterEach
  public void cleanup() {
    reset(
        messageDispatcher,
        databaseInfoService,
        eventDispatcher,
        storagesService,
        previewService,
        authenticationService);
  }

  @Nested
  @DisplayName("Get service status tests")
  class GetServiceStatusTests {

    @Test
    @DisplayName("Retrieves the service status")
    public void getServiceStatus_testOk() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.OK, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns error when the message dispatcher is not healthy")
    public void getServiceStatus_testMessageDispatcherDead() {
      when(messageDispatcher.isAlive()).thenReturn(false);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.ERROR, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns error when the database is not healthy")
    public void getServiceStatus_testDatabaseDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(false);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.ERROR, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns warn when the event dispatcher is not healthy")
    public void getServiceStatus_testEventDispatcherDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(false);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.WARN, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns warn when storages is not healthy")
    public void getServiceStatus_testStoragesDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(false);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.WARN, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns warn when the previewer is not healthy")
    public void getServiceStatus_testPreviewerDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(false);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.WARN, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns error when the authentication is not healthy")
    public void getServiceStatus_testAuthenticationDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(false);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.ERROR, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns error when the profiling service is not healthy")
    public void getServiceStatus_testProfilingDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(false);
      when(videoServerService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.ERROR, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName("Returns error when the videoserver service is not healthy")
    public void getServiceStatus_testVideoServerDead() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(false);

      assertEquals(HealthStatusTypeDto.WARN, healthcheckService.getServiceStatus());
    }

    @Test
    @DisplayName(
        "Returns error when both a non-fundamental service and a fundamental one are not healthy")
    public void getServiceStatus_testErrorOnMoreThanOne() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(false);
      when(authenticationService.isAlive()).thenReturn(false);
      when(profilingService.isAlive()).thenReturn(true);

      assertEquals(HealthStatusTypeDto.ERROR, healthcheckService.getServiceStatus());
    }
  }

  @Nested
  @DisplayName("Get service health tests")
  class GetServiceHealthTests {

    @Test
    @DisplayName("Returns the service health status when everything is healthy")
    public void getServiceHealth_testOk() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      List<DependencyHealthDto> dependencyHealthDto =
          List.of(
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.XMPP_SERVER)
                  .isHealthy(true),
              DependencyHealthDto.create().name(DependencyHealthTypeDto.DATABASE).isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.EVENT_DISPATCHER)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.STORAGE_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PREVIEWER_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.AUTHENTICATION_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PROFILING_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.VIDEOSERVER_SERVICE)
                  .isHealthy(true));
      HealthStatusDto serviceHealth = healthcheckService.getServiceHealth();
      assertTrue(serviceHealth.isIsLive());
      assertEquals(HealthStatusTypeDto.OK, serviceHealth.getStatus());
      assertTrue(dependencyHealthDto.containsAll(serviceHealth.getDependencies()));
    }

    @Test
    @DisplayName("Returns the service health status when a fundamental service is not healthy")
    public void getServiceHealth_testFundamentalNotHealthy() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(true);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(false);
      when(videoServerService.isAlive()).thenReturn(true);

      List<DependencyHealthDto> dependencyHealthDto =
          List.of(
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.XMPP_SERVER)
                  .isHealthy(true),
              DependencyHealthDto.create().name(DependencyHealthTypeDto.DATABASE).isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.EVENT_DISPATCHER)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.STORAGE_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PREVIEWER_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.AUTHENTICATION_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PROFILING_SERVICE)
                  .isHealthy(false),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.VIDEOSERVER_SERVICE)
                  .isHealthy(true));
      HealthStatusDto serviceHealth = healthcheckService.getServiceHealth();
      assertTrue(serviceHealth.isIsLive());
      assertEquals(HealthStatusTypeDto.ERROR, serviceHealth.getStatus());
      assertTrue(dependencyHealthDto.containsAll(serviceHealth.getDependencies()));
    }

    @Test
    @DisplayName("Returns the service health status when a fundamental service is not healthy")
    public void getServiceHealth_testNonFundamentalNotHealthy() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(false);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(true);
      when(videoServerService.isAlive()).thenReturn(true);

      List<DependencyHealthDto> dependencyHealthDto =
          List.of(
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.XMPP_SERVER)
                  .isHealthy(true),
              DependencyHealthDto.create().name(DependencyHealthTypeDto.DATABASE).isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.EVENT_DISPATCHER)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.STORAGE_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PREVIEWER_SERVICE)
                  .isHealthy(false),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.AUTHENTICATION_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PROFILING_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.VIDEOSERVER_SERVICE)
                  .isHealthy(true));
      HealthStatusDto serviceHealth = healthcheckService.getServiceHealth();
      assertTrue(serviceHealth.isIsLive());
      assertEquals(HealthStatusTypeDto.WARN, serviceHealth.getStatus());
      assertTrue(dependencyHealthDto.containsAll(serviceHealth.getDependencies()));
    }

    @Test
    @DisplayName(
        "Returns the service health status when both a non-fundamental and a fundamental service"
            + " are not healthy")
    public void getServiceHealth_testBothNonHealthy() {
      when(messageDispatcher.isAlive()).thenReturn(true);
      when(databaseInfoService.isAlive()).thenReturn(true);
      when(eventDispatcher.isAlive()).thenReturn(true);
      when(storagesService.isAlive()).thenReturn(true);
      when(previewService.isAlive()).thenReturn(false);
      when(authenticationService.isAlive()).thenReturn(true);
      when(profilingService.isAlive()).thenReturn(false);
      when(videoServerService.isAlive()).thenReturn(true);

      List<DependencyHealthDto> dependencyHealthDto =
          List.of(
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.XMPP_SERVER)
                  .isHealthy(true),
              DependencyHealthDto.create().name(DependencyHealthTypeDto.DATABASE).isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.EVENT_DISPATCHER)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.STORAGE_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PREVIEWER_SERVICE)
                  .isHealthy(false),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.AUTHENTICATION_SERVICE)
                  .isHealthy(true),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.PROFILING_SERVICE)
                  .isHealthy(false),
              DependencyHealthDto.create()
                  .name(DependencyHealthTypeDto.VIDEOSERVER_SERVICE)
                  .isHealthy(true));
      HealthStatusDto serviceHealth = healthcheckService.getServiceHealth();
      assertTrue(serviceHealth.isIsLive());
      assertEquals(HealthStatusTypeDto.ERROR, serviceHealth.getStatus());
      assertTrue(dependencyHealthDto.containsAll(serviceHealth.getDependencies()));
    }
  }
}
