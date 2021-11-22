package com.zextras.team.boot.config;

import com.zextras.team.core.config.CoreModule;
import com.zextras.team.core.utils.EnvConfig;
import java.util.Properties;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;

public class BootModule extends RequestScopeModule {

  private final Properties properties;

  public BootModule(Properties properties) {
    super();
    this.properties = properties;
  }

  @Override
  protected void configure() {
    super.configure();
    bind(EnvConfig.class).toInstance(new EnvConfig(properties));
    install(new CoreModule(properties));
  }
}
