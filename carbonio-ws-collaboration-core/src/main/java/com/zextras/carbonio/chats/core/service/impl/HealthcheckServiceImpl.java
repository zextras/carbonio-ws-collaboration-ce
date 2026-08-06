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
    return checkServiceStatus(snapshotDependencies());
  }

  @Override
  public HealthStatusDto getServiceHealth() {
    List<DependencySnapshot> snapshot = snapshotDependencies();
    return HealthStatusDto.create()
        .isLive(true)
        .status(checkServiceStatus(snapshot))
        .dependencies(
            snapshot.stream()
                .map(
                    entry ->
                        DependencyHealthDto.create()
                            .name(entry.dependency().getDependencyHealthType())
                            .isHealthy(entry.alive()))
                .toList());
  }

  /**
   * Calls {@code isAlive()} on every dependency exactly once and snapshots the result. Each
   * dependency is a real network round trip (e.g. to User Management), so without this the
   * required/optional passes below and the DTO mapping would each re-check every dependency,
   * turning a single {@code /health} request into several redundant calls per dependency.
   */
  private List<DependencySnapshot> snapshotDependencies() {
    return dependencies.stream()
        .map(dependency -> new DependencySnapshot(dependency, dependency.isAlive()))
        .toList();
  }

  private HealthStatusTypeDto checkServiceStatus(List<DependencySnapshot> snapshot) {
    if (snapshot.stream()
        .anyMatch(entry -> entry.dependency().getType().isRequired() && !entry.alive())) {
      return HealthStatusTypeDto.ERROR;
    } else if (snapshot.stream()
        .anyMatch(entry -> !entry.dependency().getType().isRequired() && !entry.alive())) {
      return HealthStatusTypeDto.WARN;
    }
    return HealthStatusTypeDto.OK;
  }

  private record DependencySnapshot(HealthDependency dependency, boolean alive) {}

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
