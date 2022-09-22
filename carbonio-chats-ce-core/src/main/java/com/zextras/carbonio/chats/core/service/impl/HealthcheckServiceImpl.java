// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.infrastructure.DependencyType;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.previewer.PreviewerService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.model.DependencyHealthDto;
import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HealthcheckServiceImpl implements HealthcheckService {

  private final List<HealthDependency> dependencies;

  @Inject
  public HealthcheckServiceImpl(
    MessageDispatcher messageDispatcher,
    DatabaseInfoService databaseInfoService,
    EventDispatcher eventDispatcher,
    StoragesService storagesService,
    PreviewerService previewerService,
    AuthenticationService authenticationService,
    ProfilingService profilingService
  ) {
    dependencies = List.of(
      HealthDependency.create(databaseInfoService, DependencyType.DATABASE),
      HealthDependency.create(authenticationService, DependencyType.AUTHENTICATION_SERVICE),
      HealthDependency.create(profilingService, DependencyType.PROFILING_SERVICE),
      HealthDependency.create(messageDispatcher, DependencyType.XMPP_SERVER),
      HealthDependency.create(eventDispatcher, DependencyType.EVENT_DISPATCHER),
      HealthDependency.create(storagesService, DependencyType.STORAGE_SERVICE),
      HealthDependency.create(previewerService, DependencyType.PREVIEWER_SERVICE)
    );
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
      .dependencies(dependencies.stream()
        .map(dependency -> DependencyHealthDto.create().name(dependency.getDependencyHealthType()).isHealthy(dependency.isAlive()))
        .collect(Collectors.toList()));
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
    private final DependencyType  type;

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
