// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.previewer.PreviewerService;
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
    AuthenticationService authenticationService
  ) {
    dependencies = List.of(
      HealthDependency.create(databaseInfoService, DependencyHealthTypeDto.DATABASE, true),
      HealthDependency.create(authenticationService, DependencyHealthTypeDto.ACCOUNT_SERVICE, true),
      HealthDependency.create(messageDispatcher, DependencyHealthTypeDto.XMPP_SERVER, true),
      HealthDependency.create(eventDispatcher, DependencyHealthTypeDto.EVENT_DISPATCHER, false),
      HealthDependency.create(storagesService, DependencyHealthTypeDto.STORAGE_SERVICE, false),
      HealthDependency.create(previewerService, DependencyHealthTypeDto.PREVIEWER_SERVICE, false)
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
        .map(dependency -> DependencyHealthDto.create().name(dependency.getType()).isHealthy(dependency.isAlive()))
        .collect(Collectors.toList()));
  }

  private HealthStatusTypeDto checkServiceStatus() {
    if (dependencies.stream()
      .anyMatch(dependency -> dependency.isFundamental() && !dependency.isAlive())) {
      return HealthStatusTypeDto.ERROR;
    } else if (dependencies.stream()
      .anyMatch(dependency -> !dependency.isFundamental() && !dependency.isAlive())) {
      return HealthStatusTypeDto.WARN;
    }
    return HealthStatusTypeDto.OK;
  }

  private static class HealthDependency {

    private final HealthIndicator         service;
    private final DependencyHealthTypeDto type;
    private final boolean                 fundamental;

    public HealthDependency(
      HealthIndicator dependency, DependencyHealthTypeDto type, boolean fundamental
    ) {
      this.service = dependency;
      this.type = type;
      this.fundamental = fundamental;
    }

    public static HealthDependency create(
      HealthIndicator dependency, DependencyHealthTypeDto type, boolean fundamental
    ) {
      return new HealthDependency(dependency, type, fundamental);
    }

    public boolean isAlive() {
      return service.isAlive();
    }

    public DependencyHealthTypeDto getType() {
      return type;
    }

    public boolean isFundamental() {
      return fundamental;
    }
  }

}
