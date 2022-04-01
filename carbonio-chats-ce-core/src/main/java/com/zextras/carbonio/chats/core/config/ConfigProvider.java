package com.zextras.carbonio.chats.core.config;

import com.zextras.carbonio.chats.core.config.impl.DotenvAppConfig;
import com.zextras.carbonio.chats.core.config.impl.PropertiesAppConfig;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import javax.inject.Provider;

public class ConfigProvider implements Provider<AppConfig> {

  private final Path envDirectory;
  private final Path propertiesPath;

  public ConfigProvider(Path envDirectory, Path propertiesPath) {
    this.envDirectory = envDirectory;
    this.propertiesPath = propertiesPath;
  }

  @Override
  public AppConfig get() {
    /*
    Config here was made to be as flexible as possible, one can set a config in the following ways, ordered by priority:

    1 - environment variables (for their vast interoperability)
    2 - .env file (for local tests)
    3 - deployed properties file (for packages)
    4 - Consul KV values
     */

    Dotenv dotenv = Dotenv.configure()
      .ignoreIfMissing()
      .directory(envDirectory.toString())
      .filename(".env")
      .load();
    ChatsLogger.info("Env config loaded");

    Properties properties = new Properties();
    try(InputStream propertiesStream = new FileInputStream(propertiesPath.toString())) {
      properties.load(propertiesStream);
      ChatsLogger.info("Properties config loaded");
    } catch (Exception e) {
      ChatsLogger.warn("Could not load properties file: " + e.getMessage());
    }

    AppConfig mainConfig = new DotenvAppConfig(dotenv);
    mainConfig
      .or(new PropertiesAppConfig(properties));
    return mainConfig;
  }

}
