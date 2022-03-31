// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot.config;

import com.ecwid.consul.v1.ConsulClient;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.CoreModule;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.DotenvAppConfig;
import com.zextras.carbonio.chats.core.config.impl.PropertiesAppConfig;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;

public class BootModule extends RequestScopeModule {

  public BootModule() {
    super();
  }

  @Override
  protected void configure() {
    super.configure();
    install(new CoreModule());
  }

  @Provides
  @Singleton
  public AppConfig getAppConfig() {
    /*
    Config here was made to be as flexible as possible, one can set a config in the following ways, ordered by priority:

    1 - environment variables (for their vast interoperability)
    2 - .env file (for local tests)
    3 - deployed properties file (for packages)
    4 - Consul KV values
     */

    Dotenv dotenv = Dotenv.configure()
      .ignoreIfMissing()
      .directory("./")
      .filename(".env")
      .load();
    ChatsLogger.info("Env config loaded");

    Properties properties = new Properties();
    try(InputStream propertiesStream = new FileInputStream(ChatsConstant.CONFIG_PATH)) {
      properties.load(propertiesStream);
      ChatsLogger.info("Properties config loaded");
    } catch (Exception e) {
      ChatsLogger.debug("Could not load properties file: " + e.getMessage());
    }

    AppConfig mainConfig = new DotenvAppConfig(dotenv);
    mainConfig
      .or(new PropertiesAppConfig(properties))
      .or(new ConsulAppConfig(new ConsulClient()));
    return mainConfig;
  }
}
