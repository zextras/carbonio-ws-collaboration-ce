package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.model.DependencyHealthDto;
import com.zextras.carbonio.chats.core.model.HealthResponseDto;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.core.infrastructure.dispatcher.EventDispatcher;
import java.util.ArrayList;
import javax.inject.Inject;

public class HealthcheckServiceImpl implements HealthcheckService {

  private final MessageDispatcher   messageService;
  private final DatabaseInfoService databaseInfoService;
  private final EventDispatcher     eventDispatcher;

  @Inject
  public HealthcheckServiceImpl(
    MessageDispatcher messageDispatcher,
    DatabaseInfoService databaseInfoService,
    EventDispatcher eventDispatcher
  ) {
    this.messageService = messageDispatcher;
    this.databaseInfoService = databaseInfoService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public boolean isServiceReady() {
    //if one of these services is not available, we're not ready to respond to requests
    return messageService.isAlive() &&
      databaseInfoService.isAlive() &&
      eventDispatcher.isAlive();
  }

  @Override
  public HealthResponseDto getServiceHealth() {
    HealthResponseDto healthResponseDto = new HealthResponseDto();
    healthResponseDto.setIsLive(true);
    healthResponseDto.setIsReady(true);

    ArrayList<DependencyHealthDto> dependencies = new ArrayList<>();

    //Database check
    DependencyHealthDto dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName("database");
    dependencyHealthDto.setIsHealthy(databaseInfoService.isAlive());
    dependencies.add(dependencyHealthDto);

    //XMPP Server check
    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName("xmpp server");
    dependencyHealthDto.setIsHealthy(messageService.isAlive());
    dependencies.add(dependencyHealthDto);

    //Event dispatcher check
    dependencyHealthDto = new DependencyHealthDto();
    dependencyHealthDto.setName("event dispatcher");
    dependencyHealthDto.setIsHealthy(eventDispatcher.isAlive());
    dependencies.add(dependencyHealthDto);

    healthResponseDto.setDependencies(dependencies);
    return healthResponseDto;
  }
}
