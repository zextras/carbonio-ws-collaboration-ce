package com.zextras.carbonio.chats.core.config;

import com.ecwid.consul.v1.ConsulClient;
import com.google.inject.Provider;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.DotenvAppConfig;
import com.zextras.carbonio.chats.core.config.impl.PropertiesAppConfig;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import java.nio.file.Path;

public class AppConfigProvider implements Provider<AppConfig> {

  private final String       environmentPath;
  private final String       propertiesPath;
  private       ConsulClient consulClient;

  public AppConfigProvider(String environmentPath, String propertiesPath) {
    this.environmentPath = environmentPath;
    this.propertiesPath = propertiesPath;
  }

  public AppConfigProvider(String environmentPath, String propertiesPath, ConsulClient consulClient) {
    this.environmentPath = environmentPath;
    this.propertiesPath = propertiesPath;
    this.consulClient = consulClient;
  }

  @Override
  public AppConfig get() {
    AppConfigBuilder builder = AppConfigBuilder.create()
      .add(DotenvAppConfig.create(Path.of(environmentPath)))
      .add(PropertiesAppConfig.create(Path.of(propertiesPath)));
    if (!builder.hasConfig()) {
      throw new InternalErrorException("No configurations found");
    }
    if (consulClient == null) {
      builder.add(ConsulAppConfig.create(
        builder.getAppConfig().get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
        builder.getAppConfig().get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
        builder.getAppConfig().get(String.class, ConfigName.CONSUL_TOKEN).orElse(null)));
    } else {
      builder.add(ConsulAppConfig.create(
        consulClient,
        builder.getAppConfig().get(String.class, ConfigName.CONSUL_TOKEN).orElse(null)));
    }
    return builder.getAppConfig();
  }
}
