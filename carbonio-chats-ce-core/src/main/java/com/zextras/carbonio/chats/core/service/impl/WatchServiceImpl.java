package com.zextras.carbonio.chats.core.service.impl;

import com.ecwid.consul.v1.kv.model.GetValue;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.service.WatchService;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WatchServiceImpl implements WatchService {

  private final AppConfig appConfig;

  @Inject
  public WatchServiceImpl(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public void setConsulProperties(List<GetValue> consulPropertyDto) {
    ConsulAppConfig consulConfig = (ConsulAppConfig) this.appConfig.getFromTypes(AppConfigType.CONSUL)
      .orElseThrow(() -> new NotFoundException("Consul properties management has not been started"));
    consulPropertyDto.forEach(config ->
      ConfigName.getByName(config.getKey(), AppConfigType.CONSUL).ifPresent(configName ->
        consulConfig.set(configName, config.getDecodedValue())));
  }
}
