package com.zextras.carbonio.chats.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.api.AttachmentsApi;
import com.zextras.carbonio.chats.core.api.AttachmentsApiService;
import com.zextras.carbonio.chats.core.api.HealthApi;
import com.zextras.carbonio.chats.core.api.HealthApiService;
import com.zextras.carbonio.chats.core.api.RoomsApi;
import com.zextras.carbonio.chats.core.api.RoomsApiService;
import com.zextras.carbonio.chats.core.api.UsersApi;
import com.zextras.carbonio.chats.core.api.UsersApiService;
import com.zextras.carbonio.chats.core.exception.handler.ChatsHttpExceptionHandler;
import com.zextras.carbonio.chats.core.exception.handler.DefaultExceptionHandler;
import com.zextras.carbonio.chats.core.exception.handler.XmppServerExceptionHandler;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.database.impl.EbeanDatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.MockEventDispatcherImpl;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.MessageDispatcherImpl;
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import com.zextras.carbonio.chats.core.infrastructure.storage.impl.SlimstoreStorageServiceImpl;
import com.zextras.carbonio.chats.core.invoker.RFC3339DateFormat;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.RoomMapperImpl;
import com.zextras.carbonio.chats.core.mapper.RoomUserSettingsMapper;
import com.zextras.carbonio.chats.core.mapper.RoomUserSettingsMapperImpl;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapperImpl;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanFileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanSubscriptionRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.service.impl.AttachmentServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.HealthcheckServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.MembersServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.RoomServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.UserServiceImpl;
import com.zextras.carbonio.chats.core.web.api.AttachmentsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.HealthApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.RoomsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.UsersApiServiceImpl;
import com.zextras.carbonio.chats.core.web.controller.TestController;
import com.zextras.carbonio.chats.core.web.security.AccountService;
import com.zextras.carbonio.chats.core.web.security.MockSecurityContext;
import com.zextras.carbonio.chats.core.web.security.impl.MockAccountServiceImpl;
import com.zextras.carbonio.chats.core.web.security.impl.MockSecurityContextImpl;
import com.zextras.carbonio.chats.mongooseim.admin.api.CommandsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.ApiClient;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.Configuration;
import com.zextras.filestore.api.Filestore;
import com.zextras.slimstore.api.SlimstoreClient;
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

    bind(AccountService.class).to(MockAccountServiceImpl.class);

    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);
    bind(RoomRepository.class).to(EbeanRoomRepository.class);
    bind(RoomMapper.class).to(RoomMapperImpl.class);
    bind(RoomService.class).to(RoomServiceImpl.class);

    bind(AttachmentsApi.class);
    bind(AttachmentsApiService.class).to(AttachmentsApiServiceImpl.class);
    bind(AttachmentService.class).to(AttachmentServiceImpl.class);
    bind(AttachmentMapper.class).to(AttachmentMapperImpl.class);
    bind(FileMetadataRepository.class).to(EbeanFileMetadataRepository.class);

    bind(UsersApi.class);
    bind(UsersApiService.class).to(UsersApiServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);

    bind(HealthApi.class);
    bind(HealthApiService.class).to(HealthApiServiceImpl.class);
    bind(HealthcheckService.class).to(HealthcheckServiceImpl.class);
    bind(DatabaseInfoService.class).to(EbeanDatabaseInfoService.class);

    bind(MembersService.class).to(MembersServiceImpl.class);
    bind(SubscriptionRepository.class).to(EbeanSubscriptionRepository.class);
    bind(SubscriptionMapper.class).to(SubscriptionMapperImpl.class);

    bind(RoomUserSettingsRepository.class).to(EbeanRoomUserSettingsRepository.class);
    bind(RoomUserSettingsMapper.class).to(RoomUserSettingsMapperImpl.class);

    bind(MessageDispatcher.class).to(MessageDispatcherImpl.class);
    bind(StorageService.class).to(SlimstoreStorageServiceImpl.class);

    bind(TestController.class);
    bindExceptionMapper();
  }

  private void bindExceptionMapper() {
    bind(ChatsHttpExceptionHandler.class);
    bind(XmppServerExceptionHandler.class);
    bind(DefaultExceptionHandler.class);
  }

  @Singleton
  @Provides
  private MucLightManagementApi initMongooseImMucLight(AppConfig appConfig) {
    Configuration.setDefaultApiClient(new ApiClient()
      .setBasePath(appConfig.get(String.class, "MONGOOSEIM_ADMIN_REST_BASE_URL").orElseThrow())
      .addDefaultHeader("Accept", "*/*")
      .setDebugging(true));
    com.zextras.carbonio.chats.mongooseim.client.invoker.Configuration.setDefaultApiClient(
      new com.zextras.carbonio.chats.mongooseim.client.invoker.ApiClient()
        .setBasePath(appConfig.get(String.class, "MONGOOSEIM_CLIENT_REST_BASE_URL").orElseThrow())
        .addDefaultHeader("Accept", "*/*")
        .setDebugging(true));
    return new MucLightManagementApi();
  }

  @Singleton
  @Provides
  private CommandsApi initMongooseImCommands(AppConfig appConfig) {
    Configuration.setDefaultApiClient(new ApiClient()
      .setBasePath(appConfig.get(String.class, "MONGOOSEIM_ADMIN_REST_BASE_URL").orElseThrow())
      .addDefaultHeader("Accept", "*/*")
      .setDebugging(true));
    com.zextras.carbonio.chats.mongooseim.client.invoker.Configuration.setDefaultApiClient(
      new com.zextras.carbonio.chats.mongooseim.client.invoker.ApiClient()
        .setBasePath(appConfig.get(String.class, "MONGOOSEIM_CLIENT_REST_BASE_URL").orElseThrow())
        .addDefaultHeader("Accept", "*/*")
        .setDebugging(true));
    return new CommandsApi();
  }

  @Singleton
  @Provides
  private Filestore getSlimstorClient(AppConfig appConfig) {
    return SlimstoreClient.atUrl(appConfig.get(String.class, "FILESTORE_URL").orElseThrow());
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
