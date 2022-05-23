package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.service.CosulService;
import com.zextras.carbonio.chats.model.ConsulPropertyDto;
import java.util.Base64;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CosulServiceImpl implements CosulService {

  private final AppConfig appConfig;

  @Inject
  public CosulServiceImpl(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public void setConsulProperties(List<ConsulPropertyDto> consulPropertyDto) {
    AppConfig consulConfig = this.appConfig.getFromTypes(AppConfigType.CONSUL)
      .orElseThrow(() -> new NotFoundException("Consul properties management was not started"));
    consulPropertyDto.forEach(config ->
      ConfigName.getByName(config.getKey(), AppConfigType.CONSUL).ifPresent(configName ->
        consulConfig.set(configName, new String(Base64.getDecoder().decode(config.getValue())))));
  }
}
