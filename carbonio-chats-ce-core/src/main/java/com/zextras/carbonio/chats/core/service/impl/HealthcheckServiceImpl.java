// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.model.DependencyHealthDto;

import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;
import com.zextras.carbonio.chats.model.HealthStatusDto;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HealthcheckServiceImpl implements HealthcheckService {

  private final MessageDispatcher   messageService;
  private final DatabaseInfoService databaseInfoService;
  private final EventDispatcher     eventDispatcher;
  private final StorageService      storageService;

  @Inject
  public HealthcheckServiceImpl(
    MessageDispatcher messageDispatcher,
    DatabaseInfoService databaseInfoService,
    EventDispatcher eventDispatcher,
    StorageService storageService
  ) {
    this.messageService = messageDispatcher;
    this.databaseInfoService = databaseInfoService;
    this.eventDispatcher = eventDispatcher;
    this.storageService = storageService;
  }

  @Override
  public boolean isServiceReady() {
    //if one of these services is not available, we're not ready to respond to requests
    return messageService.isAlive() &&
      databaseInfoService.isAlive() &&
      eventDispatcher.isAlive() &&
      storageService.isAlive();
  }

  @Override
  public HealthStatusDto getServiceHealth() {
    HealthStatusDto healthResponseDto = new HealthStatusDto();
    healthResponseDto.setIsLive(true);
    healthResponseDto.setIsReady(true);

    ArrayList<DependencyHealthDto> dependencies = new ArrayList<>();

    //Database check
    DependencyHealthDto dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(DependencyHealthTypeDto.DATABASE);
    dependencyHealthDto.setIsHealthy(databaseInfoService.isAlive());
    dependencies.add(dependencyHealthDto);

    //XMPP Server check
    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(DependencyHealthTypeDto.XMPP_SERVER);
    dependencyHealthDto.setIsHealthy(messageService.isAlive());
    dependencies.add(dependencyHealthDto);

    //Event dispatcher check
    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(DependencyHealthTypeDto.EVENT_DISPATCHER);
    dependencyHealthDto.setIsHealthy(eventDispatcher.isAlive());
    dependencies.add(dependencyHealthDto);

    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(DependencyHealthTypeDto.STORAGE_SERVICE);
    dependencyHealthDto.setIsHealthy(storageService.isAlive());
    dependencies.add(dependencyHealthDto);

    healthResponseDto.setDependencies(dependencies);
    return healthResponseDto;
  }
}
