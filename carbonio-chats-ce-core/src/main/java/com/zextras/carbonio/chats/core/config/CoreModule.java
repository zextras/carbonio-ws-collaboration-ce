// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.api.AttachmentsApi;
import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.api.HealthApi;
import com.zextras.carbonio.chats.api.HealthApiService;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.api.SupportedApi;
import com.zextras.carbonio.chats.api.SupportedApiService;
import com.zextras.carbonio.chats.api.UsersApi;
import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.logging.aop.TimedCallInterceptor;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.authentication.impl.UserManagementAuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.database.impl.EbeanDatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.MockEventDispatcherImpl;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.MessageDispatcherMongooseIm;
import com.zextras.carbonio.chats.core.infrastructure.previewer.PreviewerService;
import com.zextras.carbonio.chats.core.infrastructure.previewer.impl.PreviewerServiceImpl;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.impl.UserManagementProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.storage.impl.StoragesServiceImpl;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;

import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapperImpl;
import com.zextras.carbonio.chats.core.provider.AppInfoProvider;
import com.zextras.carbonio.chats.core.provider.impl.AppInfoProviderImpl;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanFileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanSubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanUserRepository;
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
import com.zextras.carbonio.chats.core.web.api.SupportedApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.UsersApiServiceImpl;
import com.zextras.carbonio.chats.core.web.exceptions.ChatsHttpExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.ClientErrorExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.DefaultExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.JsonProcessingExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.ValidationExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.XmppServerExceptionHandler;
import com.zextras.carbonio.chats.core.web.security.AuthenticationFilter;
import com.zextras.carbonio.chats.mongooseim.admin.api.CommandsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.ContactsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.OneToOneMessagesApi;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.ApiClient;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.storages.api.StoragesClient;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import java.time.Clock;
import java.time.ZoneId;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public class CoreModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    //This is bound twice, once for RestEasy injection and one for everything else
    bind(JacksonConfig.class);
    bind(ObjectMapper.class).toProvider(JacksonConfig.class);

    bind(AppConfig.class).toProvider(new AppConfigProvider(".", ChatsConstant.CONFIG_PATH)).in(Singleton.class);

    bind(EventDispatcher.class).to(MockEventDispatcherImpl.class);
    bind(AuthenticationFilter.class);

    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);
    bind(RoomRepository.class).to(EbeanRoomRepository.class);
    bind(RoomMapper.class);
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

    bind(SupportedApi.class);
    bind(SupportedApiService.class).to(SupportedApiServiceImpl.class);
    bind(AppInfoProvider.class).to(AppInfoProviderImpl.class);

    bind(RoomUserSettingsRepository.class).to(EbeanRoomUserSettingsRepository.class);

    bind(UserService.class).to(UserServiceImpl.class);
    bind(UserRepository.class).to(EbeanUserRepository.class);

    bind(MessageDispatcher.class).to(MessageDispatcherMongooseIm.class);
    bind(StoragesService.class).to(StoragesServiceImpl.class);
    bind(PreviewerService.class).to(PreviewerServiceImpl.class);
    bind(ProfilingService.class).to(UserManagementProfilingService.class);
    bind(AuthenticationService.class).to(UserManagementAuthenticationService.class);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(TimedCall.class), new TimedCallInterceptor());

    bindExceptionMapper();
  }

  private void bindExceptionMapper() {
    bind(ChatsHttpExceptionHandler.class);
    bind(XmppServerExceptionHandler.class);
    bind(ClientErrorExceptionHandler.class);
    bind(JsonProcessingExceptionHandler.class);
    bind(DefaultExceptionHandler.class);
    bind(ValidationExceptionHandler.class);
  }

  @Singleton
  @Provides
  private Clock getClock() {
    return Clock.system(ZoneId.systemDefault());
  }

  @Singleton
  @Provides
  private ApiClient getMongooseImAdminApiClient(AppConfig appConfig) {
    return new ApiClient()
      .setBasePath(String.format("http://%s:%s/%s",
        appConfig.get(String.class, ConfigName.XMPP_SERVER_HOST).orElseThrow(),
        appConfig.get(String.class, ConfigName.XMPP_SERVER_HTTP_PORT).orElseThrow(),
        ChatsConstant.MONGOOSEIM_ADMIN_ENDPOINT))
      .addDefaultHeader("Accept", "*/*")
      .setDebugging(true);
  }

  @Singleton
  @Provides
  private MucLightManagementApi getMongooseImMucLight(ApiClient apiClient) {
    return new MucLightManagementApi(apiClient);
  }

  @Singleton
  @Provides
  private OneToOneMessagesApi getOneToOneMessageApi(ApiClient apiClient) {
    return new OneToOneMessagesApi(apiClient);
  }

  @Singleton
  @Provides
  private ContactsApi getMongooseImContacts(ApiClient apiClient) {
    return new ContactsApi(apiClient);
  }

  @Singleton
  @Provides
  private StoragesClient getStoragesClient(AppConfig appConfig) {
    return StoragesClient.atUrl(
      String.format("http://%s:%s",
        appConfig.get(String.class, ConfigName.STORAGES_HOST).orElseThrow(),
        appConfig.get(String.class, ConfigName.STORAGES_PORT).orElseThrow()
      )
    );
  }

  @Singleton
  @Provides
  private PreviewClient getPreviewClient(AppConfig appConfig) {
    return PreviewClient.atURL(String.format("http://%s:%s",
      appConfig.get(String.class, ConfigName.PREVIEWER_HOST).orElseThrow(),
      appConfig.get(String.class, ConfigName.PREVIEWER_PORT).orElseThrow()));
  }

  @Singleton
  @Provides
  private UserManagementClient getUserManagementClient(AppConfig appConfig) {
    return UserManagementClient.atURL(
      String.format("http://%s:%s",
        appConfig.get(String.class, ConfigName.USER_MANAGEMENT_HOST).orElseThrow(),
        appConfig.get(String.class, ConfigName.USER_MANAGEMENT_PORT).orElseThrow()
      )
    );
  }

  @Singleton
  @Provides
  public Database getDatabase(DataSource dataSource, Clock clock) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDataSource(dataSource);
    databaseConfig.setClock(clock);
    return DatabaseFactory.create(databaseConfig);
  }

  @Singleton
  @Provides
  public Flyway getFlywayInstance(DataSource dataSource) {
    return Flyway.configure()
      .locations("classpath:migration")
      .schemas("chats")
      .dataSource(dataSource)
      .load();
  }

  @Singleton
  @Provides
  public DataSource getHikariDataSource(AppConfig appConfig) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).orElseThrow());
    config.setUsername(appConfig.get(String.class, ConfigName.DATABASE_USERNAME).orElse("chats"));
    config.setPassword(appConfig.get(String.class, ConfigName.DATABASE_PASSWORD).orElse("password"));
    config.addDataSourceProperty("idleTimeout",
      appConfig.get(Integer.class, ConfigName.HIKARI_IDLE_TIMEOUT).orElse(300));
    config.addDataSourceProperty("minimumIdle",
      appConfig.get(Integer.class, ConfigName.HIKARI_MIN_POOL_SIZE).orElse(1));
    config.addDataSourceProperty("maximumPoolSize",
      appConfig.get(Integer.class, ConfigName.HIKARI_MAX_POOL_SIZE).orElse(5));
    config.addDataSourceProperty("poolName", "chats-db-pool");
    config.addDataSourceProperty("driverClassName", appConfig.get(String.class, ConfigName.JDBC_DRIVER).orElseThrow());
    config.addDataSourceProperty("leakDetectionThreshold",
      appConfig.get(Integer.class, ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD).orElse(60000));
    return new HikariDataSource(config);
  }

  @Singleton
  @Provides
  private CommandsApi getMongooseImCommands(ApiClient apiClient) {
    return new CommandsApi(apiClient);
  }
}
