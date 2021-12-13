package com.zextras.chats.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.chats.core.api.AttachmentsApi;
import com.zextras.chats.core.api.AttachmentsApiService;
import com.zextras.chats.core.api.HealthcheckApi;
import com.zextras.chats.core.api.HealthcheckApiService;
import com.zextras.chats.core.api.RoomsApi;
import com.zextras.chats.core.api.RoomsApiService;
import com.zextras.chats.core.api.UsersApi;
import com.zextras.chats.core.api.UsersApiService;
import com.zextras.chats.core.exception.mapper.ChatsHttpExceptionHandler;
import com.zextras.chats.core.exception.mapper.DefaultExceptionHandler;
import com.zextras.chats.core.invoker.JacksonConfig;
import com.zextras.chats.core.invoker.RFC3339DateFormat;
import com.zextras.chats.core.mapper.RoomMapper;
import com.zextras.chats.core.mapper.RoomMapperImpl;
import com.zextras.chats.core.mapper.RoomUserSettingsMapper;
import com.zextras.chats.core.mapper.SubscriptionMapper;
import com.zextras.chats.core.mapper.SubscriptionMapperImpl;
import com.zextras.chats.core.mapper.impl.RoomUserSettingsMapperImpl;
import com.zextras.chats.core.repository.RoomImageRepository;
import com.zextras.chats.core.repository.RoomRepository;
import com.zextras.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.chats.core.repository.SubscriptionRepository;
import com.zextras.chats.core.repository.impl.EbeanRoomImageRepository;
import com.zextras.chats.core.repository.impl.EbeanRoomRepository;
import com.zextras.chats.core.repository.impl.EbeanRoomUserSettingsRepository;
import com.zextras.chats.core.repository.impl.EbeanSubscriptionRepository;
import com.zextras.chats.core.service.MembersService;
import com.zextras.chats.core.service.RoomPictureService;
import com.zextras.chats.core.service.impl.AttachmentsApiServiceImpl;
import com.zextras.chats.core.service.impl.HealthcheckApiServiceImpl;
import com.zextras.chats.core.service.impl.MembersServiceImpl;
import com.zextras.chats.core.service.impl.RoomPictureServiceImpl;
import com.zextras.chats.core.service.impl.RoomsApiServiceImpl;
import com.zextras.chats.core.service.impl.UsersApiServiceImpl;
import com.zextras.chats.core.web.controller.TestController;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.chats.core.web.dispatcher.MessageDispatcher;
import com.zextras.chats.core.web.dispatcher.impl.MockEventDispatcherImpl;
import com.zextras.chats.core.web.dispatcher.impl.MockMessageDispatcherImpl;
import com.zextras.chats.core.web.security.AccountService;
import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.impl.MockAccountServiceImpl;
import com.zextras.chats.core.web.security.impl.MockSecurityContextImpl;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public class CoreModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();

    bind(JacksonConfig.class);

    bind(MockSecurityContext.class).to(MockSecurityContextImpl.class);

    bind(EventDispatcher.class).to(MockEventDispatcherImpl.class);
    bind(MessageDispatcher.class).to(MockMessageDispatcherImpl.class);

    bind(AccountService.class).to(MockAccountServiceImpl.class);

    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);
    bind(RoomRepository.class).to(EbeanRoomRepository.class);
    bind(RoomMapper.class).to(RoomMapperImpl.class);

    bind(AttachmentsApi.class);
    bind(AttachmentsApiService.class).to(AttachmentsApiServiceImpl.class);

    bind(UsersApi.class);
    bind(UsersApiService.class).to(UsersApiServiceImpl.class);

    bind(HealthcheckApi.class);
    bind(HealthcheckApiService.class).to(HealthcheckApiServiceImpl.class);

    bind(MembersService.class).to(MembersServiceImpl.class);
    bind(SubscriptionRepository.class).to(EbeanSubscriptionRepository.class);
    bind(SubscriptionMapper.class).to(SubscriptionMapperImpl.class);

    bind(RoomUserSettingsRepository.class).to(EbeanRoomUserSettingsRepository.class);
    bind(RoomUserSettingsMapper.class).to(RoomUserSettingsMapperImpl.class);

    bind(RoomPictureService.class).to(RoomPictureServiceImpl.class);
    bind(RoomImageRepository.class).to(EbeanRoomImageRepository.class);

    bind(TestController.class);
    bindExceptionMapper();
  }

  private void bindExceptionMapper() {
    bind(ChatsHttpExceptionHandler.class);
    bind(DefaultExceptionHandler.class);
  }

  @Singleton
  @Provides
  public Database getDatabase(AppConfig appConfig) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDataSource(getHikariDataSource(appConfig));
    return DatabaseFactory.create(databaseConfig);
  }

  @Singleton
  @Provides
  public ObjectMapper getObjectMapper() {
    return new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .setDateFormat(new RFC3339DateFormat());
  }

  @Singleton
  @Provides
  public Flyway getFlywayInstance(AppConfig appConfig) {
    return Flyway.configure()
      .locations("classpath:migration")
      .schemas("chats")
      .dataSource(getHikariDataSource(appConfig))
      .load();
  }

  private DataSource getHikariDataSource(AppConfig appConfig) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(appConfig.get(String.class, "DATABASE_JDBC_URL").orElseThrow());
    config.setUsername(appConfig.get(String.class, "DATABASE_USERNAME").orElse("chats"));
    config.setPassword(appConfig.get(String.class, "DATABASE_PASSWORD").orElse("password"));
    config.addDataSourceProperty("idleTimeout", appConfig.get(Integer.class, "HIKARI_IDLE_TIMEOUT").orElse(300));
    config.addDataSourceProperty("minimumIdle", appConfig.get(Integer.class, "HIKARI_MIN_POOL_SIZE").orElse(1));
    config.addDataSourceProperty("maximumPoolSize", appConfig.get(Integer.class, "HIKARI_MAX_POOL_SIZE").orElse(5));
    config.addDataSourceProperty("poolName", "chats-db-pool");
    config.addDataSourceProperty("driverClassName", appConfig.get(String.class, "DATASOURCE_DRIVER").orElseThrow());
    config.addDataSourceProperty("leakDetectionThreshold",
      appConfig.get(Integer.class, "HIKARI_LEAK_DETECTION_THRESHOLD").orElse(60000));
    return new HikariDataSource(config);
  }

}
