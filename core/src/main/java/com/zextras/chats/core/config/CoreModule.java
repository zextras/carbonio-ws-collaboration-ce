package com.zextras.chats.core.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.chats.core.api.AttachmentsApi;
import com.zextras.chats.core.api.AttachmentsApiService;
import com.zextras.chats.core.api.RoomsApi;
import com.zextras.chats.core.api.RoomsApiService;
import com.zextras.chats.core.api.UsersApi;
import com.zextras.chats.core.api.UsersApiService;
import com.zextras.chats.core.exception.mapper.DefaultExceptionHandler;
import com.zextras.chats.core.exception.mapper.ChatsHttpExceptionHandler;
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
import com.zextras.chats.core.service.impl.MembersServiceImpl;
import com.zextras.chats.core.service.impl.RoomPictureServiceImpl;
import com.zextras.chats.core.service.impl.RoomsApiServiceImpl;
import com.zextras.chats.core.service.impl.UsersApiServiceImpl;
import com.zextras.chats.core.web.controller.TestController;
import com.zextras.chats.core.web.security.AccountService;
import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.impl.MockAccountServiceImpl;
import com.zextras.chats.core.web.security.impl.MockSecurityContextImpl;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.chats.core.web.dispatcher.MessageDispatcher;
import com.zextras.chats.core.web.dispatcher.impl.MockEventDispatcherImpl;
import com.zextras.chats.core.web.dispatcher.impl.MockMessageDispatcherImpl;
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

    //TODO we should move this away from here
    Flyway flyway = Flyway.configure()
      .locations("classpath:migration")
      .schemas("chats")
      .dataSource(getHikariDataSource())
      .load();
    flyway.migrate();

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
  public Database getDatabase() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDataSource(getHikariDataSource());
    return DatabaseFactory.create(databaseConfig);
  }

  private DataSource getHikariDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(properties.getProperty("zextras.chats.datasource.jdbcUrl"));
    config.setUsername(properties.getProperty("zextras.chats.datasource.username"));
    config.setPassword(properties.getProperty("zextras.chats.datasource.password"));
    config.addDataSourceProperty("idleTimeout", properties.getProperty("zextras.chats.datasource.idleTimeout"));
    config.addDataSourceProperty("minimumIdle", properties.getProperty("zextras.chats.datasource.minPoolSize"));
    config.addDataSourceProperty("maximumPoolSize", properties.getProperty("zextras.chats.datasource.maxPoolSize"));
    config.addDataSourceProperty("poolName", properties.getProperty("zextras.chats.datasource.poolName"));
    config.addDataSourceProperty("driverClassName", properties.getProperty("zextras.chats.datasource.driverClassName"));
    config.addDataSourceProperty("leakDetectionThreshold",
      properties.getProperty("zextras.chats.datasource.leakDetectionThreshold"));
    return new HikariDataSource(config);
  }

}
