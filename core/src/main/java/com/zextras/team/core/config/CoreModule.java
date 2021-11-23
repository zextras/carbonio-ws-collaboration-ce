package com.zextras.team.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.team.core.api.AttachmentsApi;
import com.zextras.team.core.api.AttachmentsApiService;
import com.zextras.team.core.api.RoomsApi;
import com.zextras.team.core.api.RoomsApiService;
import com.zextras.team.core.api.UsersApi;
import com.zextras.team.core.api.UsersApiService;
import com.zextras.team.core.exception.mapper.DefaultExceptionHandler;
import com.zextras.team.core.exception.mapper.TeamHttpExceptionHandler;
import com.zextras.team.core.repository.RoomRepository;
import com.zextras.team.core.repository.SubscriptionRepository;
import com.zextras.team.core.repository.UserRepository;
import com.zextras.team.core.repository.impl.EbeanRoomRepository;
import com.zextras.team.core.repository.impl.EbeanSubscriptionRepository;
import com.zextras.team.core.repository.impl.EbeanUserRepository;
import com.zextras.team.core.service.impl.AttachmentsApiServiceImpl;
import com.zextras.team.core.service.impl.RoomsApiServiceImpl;
import com.zextras.team.core.service.impl.UsersApiServiceImpl;
import com.zextras.team.core.web.controller.TestController;
import com.zextras.team.core.web.dispatcher.EventDispatcher;
import com.zextras.team.core.web.dispatcher.MessageDispatcher;
import com.zextras.team.core.web.dispatcher.impl.MockEventDispatcherImpl;
import com.zextras.team.core.web.dispatcher.impl.MockMessageDispatcherImpl;
import com.zextras.team.core.web.security.AccountService;
import com.zextras.team.core.web.security.MockSecurityContext;
import com.zextras.team.core.web.security.impl.MockAccountServiceImpl;
import com.zextras.team.core.web.security.impl.MockSecurityContextImpl;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import java.util.Properties;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public class CoreModule extends AbstractModule {

  private final Properties properties;

  public CoreModule() {
    super();
    properties = new Properties();
  }

  public CoreModule(Properties properties) {
    super();
    this.properties = properties;
  }

  @Override
  protected void configure() {
    super.configure();

    Flyway flyway = Flyway.configure().dataSource(getHikariDataSource()).load();
    flyway.migrate();

    bind(ObjectMapper.class);
    bind(MockSecurityContext.class).to(MockSecurityContextImpl.class);

    bind(EventDispatcher.class).to(MockEventDispatcherImpl.class);
    bind(MessageDispatcher.class).to(MockMessageDispatcherImpl.class);

    bind(AccountService.class).to(MockAccountServiceImpl.class);

    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);
    bind(RoomRepository.class).to(EbeanRoomRepository.class);

    bind(AttachmentsApi.class);
    bind(AttachmentsApiService.class).to(AttachmentsApiServiceImpl.class);

    bind(UsersApi.class);
    bind(UsersApiService.class).to(UsersApiServiceImpl.class);
    bind(UserRepository.class).to(EbeanUserRepository.class);

    bind(SubscriptionRepository.class).to(EbeanSubscriptionRepository.class);

    bind(TestController.class);
    bindExceptionMapper();
  }

  private void bindExceptionMapper() {
    bind(TeamHttpExceptionHandler.class);
    bind(DefaultExceptionHandler.class);
  }

  @Singleton
  @Provides
  public Database getDatabase() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDataSource(getHikariDataSource());
    return DatabaseFactory.create(databaseConfig);
  }

  private DataSource getHikariDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(properties.getProperty("zextras.team.datasource.jdbcUrl"));
    config.setUsername(properties.getProperty("zextras.team.datasource.username"));
    config.setPassword(properties.getProperty("zextras.team.datasource.password"));
    config.addDataSourceProperty("idleTimeout", properties.getProperty("zextras.team.datasource.idleTimeout"));
    config.addDataSourceProperty("minimumIdle", properties.getProperty("zextras.team.datasource.minPoolSize"));
    config.addDataSourceProperty("maximumPoolSize", properties.getProperty("zextras.team.datasource.maxPoolSize"));
    config.addDataSourceProperty("poolName", properties.getProperty("zextras.team.datasource.poolName"));
    config.addDataSourceProperty("driverClassName", properties.getProperty("zextras.team.datasource.driverClassName"));
    config.addDataSourceProperty("leakDetectionThreshold",
      properties.getProperty("zextras.team.datasource.leakDetectionThreshold"));
    return new HikariDataSource(config);
  }

}
