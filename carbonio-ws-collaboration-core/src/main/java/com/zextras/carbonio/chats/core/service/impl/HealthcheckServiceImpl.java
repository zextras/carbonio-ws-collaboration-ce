// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.preview.PreviewService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.model.DependencyHealthDto;
import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;
import java.util.List;

@Singleton
public class HealthcheckServiceImpl implements HealthcheckService {

  private final List<HealthDependency> dependencies;

  @Inject
  public HealthcheckServiceImpl(
      MessageDispatcher messageDispatcher,
      DatabaseInfoService databaseInfoService,
      EventDispatcher eventDispatcher,
      StoragesService storagesService,
      PreviewService previewService,
      AuthenticationService authenticationService,
      ProfilingService profilingService,
      VideoServerService videoServerService) {
    dependencies =
        List.of(
            HealthDependency.create(databaseInfoService, DependencyType.DATABASE),
            HealthDependency.create(authenticationService, DependencyType.AUTHENTICATION_SERVICE),
            HealthDependency.create(profilingService, DependencyType.PROFILING_SERVICE),
            HealthDependency.create(messageDispatcher, DependencyType.XMPP_SERVER),
            HealthDependency.create(eventDispatcher, DependencyType.EVENT_DISPATCHER),
            HealthDependency.create(storagesService, DependencyType.STORAGE_SERVICE),
            HealthDependency.create(previewService, DependencyType.PREVIEWER_SERVICE),
            HealthDependency.create(videoServerService, DependencyType.VIDEOSERVER_SERVICE));
  }

  @Override
  public HealthStatusTypeDto getServiceStatus() {
    return checkServiceStatus();
  }

  @Override
  public HealthStatusDto getServiceHealth() {
    return HealthStatusDto.create()
        .isLive(true)
        .status(checkServiceStatus())
        .dependencies(
            dependencies.stream()
                .map(
                    dependency ->
                        DependencyHealthDto.create()
                            .name(dependency.getDependencyHealthType())
                            .isHealthy(dependency.isAlive()))
                .toList());
  }

  private HealthStatusTypeDto checkServiceStatus() {
    if (dependencies.stream()
        .anyMatch(dependency -> dependency.getType().isRequired() && !dependency.isAlive())) {
      return HealthStatusTypeDto.ERROR;
    } else if (dependencies.stream()
        .anyMatch(dependency -> !dependency.getType().isRequired() && !dependency.isAlive())) {
      return HealthStatusTypeDto.WARN;
    }
    return HealthStatusTypeDto.OK;
  }

  private static class HealthDependency {

    private final HealthIndicator service;
    private final DependencyType type;

    public HealthDependency(HealthIndicator dependency, DependencyType type) {
      this.service = dependency;
      this.type = type;
    }

    public static HealthDependency create(HealthIndicator dependency, DependencyType type) {
      return new HealthDependency(dependency, type);
    }

    public boolean isAlive() {
      return service.isAlive();
    }

    public DependencyType getType() {
      return type;
    }

    public DependencyHealthTypeDto getDependencyHealthType() {
      return DependencyHealthTypeDto.fromString(type.getName());
    }
  }
}
