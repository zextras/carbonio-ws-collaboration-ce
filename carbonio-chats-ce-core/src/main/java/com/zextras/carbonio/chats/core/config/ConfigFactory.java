package com.zextras.carbonio.chats.core.config;

import com.ecwid.consul.v1.ConsulClient;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.DotenvAppConfig;
import com.zextras.carbonio.chats.core.config.impl.PropertiesAppConfig;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

class ConfigFactory {

  private final Path         envDirectory;
  private final Path         propertiesPath;
  private final ConsulClient consulClient;

  public ConfigFactory(Path envDirectory, Path propertiesPath, ConsulClient consulClient) {
    this.envDirectory = envDirectory;
    this.propertiesPath = propertiesPath;
    this.consulClient = consulClient;
  }

  public AppConfig create() {
    /*
    Config here was made to be as flexible as possible, one can set a config in the following ways, ordered by priority:

    1 - environment variables (for their vast interoperability)
    2 - .env file (for local tests)
    3 - deployed properties file (for packages)
    4 - Consul KV values
     */

    //We might want to hide initialization details inside config classes themselves or inside a factory
    //to abstract even further.
    //This also might be simplified by removing the chain of responsibility part since it brings lots of complications
    //especially in testing
    Dotenv dotenv = Dotenv.configure()
      .ignoreIfMissing()
      .directory(envDirectory.toString())
      .filename(".env")
      .load();
    AppConfig mainConfig = new DotenvAppConfig(dotenv);
    ChatsLogger.info("Env config loaded");

    Properties properties = new Properties();
    try (InputStream propertiesStream = new FileInputStream(propertiesPath.toString())) {
      properties.load(propertiesStream);
      ChatsLogger.info("Properties config loaded");
    } catch (Exception e) {
      ChatsLogger.warn("Could not load properties file: " + e.getMessage());
    }

    ConsulAppConfig consulAppConfig = new ConsulAppConfig(consulClient,
      mainConfig.get(String.class, ConfigName.CONSUL_TOKEN).orElseThrow());
    try {
      consulAppConfig.loadConfigurations();
      ChatsLogger.info("Consul config loaded");
    } catch (RuntimeException ex) {
      ChatsLogger.warn(String.format("Error while loading consul config: %s", ex.getMessage()));
    }

    mainConfig
      .or(new PropertiesAppConfig(properties))
      .or(consulAppConfig);

    return mainConfig;
  }

}
