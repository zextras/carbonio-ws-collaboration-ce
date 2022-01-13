package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import com.zextras.carbonio.chats.core.model.DependencyHealthDto;
import com.zextras.carbonio.chats.core.model.HealthDependencyTypeDto;
import com.zextras.carbonio.chats.core.model.HealthResponseDto;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
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
  public HealthResponseDto getServiceHealth() {
    HealthResponseDto healthResponseDto = new HealthResponseDto();
    healthResponseDto.setIsLive(true);
    healthResponseDto.setIsReady(true);

    ArrayList<DependencyHealthDto> dependencies = new ArrayList<>();

    //Database check
    DependencyHealthDto dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(HealthDependencyTypeDto.DATABASE);
    dependencyHealthDto.setIsHealthy(databaseInfoService.isAlive());
    dependencies.add(dependencyHealthDto);

    //XMPP Server check
    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(HealthDependencyTypeDto.XMPP_SERVER);
    dependencyHealthDto.setIsHealthy(messageService.isAlive());
    dependencies.add(dependencyHealthDto);

    //Event dispatcher check
    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(HealthDependencyTypeDto.EVENT_DISPATCHER);
    dependencyHealthDto.setIsHealthy(eventDispatcher.isAlive());
    dependencies.add(dependencyHealthDto);

    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName(HealthDependencyTypeDto.STORAGE_SERVICE);
    dependencyHealthDto.setIsHealthy(storageService.isAlive());
    dependencies.add(dependencyHealthDto);

    healthResponseDto.setDependencies(dependencies);
    return healthResponseDto;
  }
}
