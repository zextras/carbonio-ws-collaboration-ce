// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.api.AttachmentsApi;
import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.api.AuthApi;
import com.zextras.carbonio.chats.api.AuthApiService;
import com.zextras.carbonio.chats.api.HealthApi;
import com.zextras.carbonio.chats.api.HealthApiService;
import com.zextras.carbonio.chats.api.PreviewApi;
import com.zextras.carbonio.chats.api.PreviewApiService;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.api.UsersApi;
import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.cache.CacheHandler;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.InfrastructureAppConfig;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.authentication.impl.UserManagementAuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.database.impl.EbeanDatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.EventDispatcherRabbitMq;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.MessageDispatcherMongooseImpl;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.profiling.impl.UserManagementProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.storage.impl.StoragesServiceImpl;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerConfig;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerConfigImpl;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerHttpClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.logging.aop.TimedCallInterceptor;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.impl.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.RoomMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.SubscriptionMapperImpl;
import com.zextras.carbonio.chats.core.provider.AppInfoProvider;
import com.zextras.carbonio.chats.core.provider.impl.AppInfoProviderImpl;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanFileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMeetingRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanParticipantRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanSubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanUserRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanVideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanVideoServerSessionRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.PreviewService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.service.impl.AttachmentServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.CapabilityServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.HealthcheckServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.MembersServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.ParticipantServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.PreviewServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.RoomServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.UserServiceImpl;
import com.zextras.carbonio.chats.core.web.api.AttachmentsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.AuthApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.HealthApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.MeetingsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.PreviewApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.RoomsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.UsersApiServiceImpl;
import com.zextras.carbonio.chats.core.web.exceptions.ChatsHttpExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.ClientErrorExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.DefaultExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.JsonProcessingExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.ValidationExceptionHandler;
import com.zextras.carbonio.chats.core.web.security.AuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.meeting.api.MeetingsApi;
import com.zextras.carbonio.meeting.api.MeetingsApiService;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.storages.api.StoragesClient;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.config.DatabaseConfig;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.flywaydb.core.Flyway;

public class CoreModule extends AbstractModule {

  private static final String URL_PATTERN = "http://%s:%s";

  @Override
  protected void configure() {
    super.configure();
    // This is bound twice, once for RestEasy injection and one for everything else
    bind(JacksonConfig.class);
    bind(ObjectMapper.class).toProvider(JacksonConfig.class);

    bind(AuthenticationFilter.class);
    bind(EventDispatcher.class).to(EventDispatcherRabbitMq.class);
    bind(EventsWebSocketManager.class);

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

    bind(PreviewApi.class);
    bind(PreviewApiService.class).to(PreviewApiServiceImpl.class);
    bind(PreviewService.class).to(PreviewServiceImpl.class);

    bind(HealthApi.class);
    bind(HealthApiService.class).to(HealthApiServiceImpl.class);
    bind(HealthcheckService.class).to(HealthcheckServiceImpl.class);
    bind(DatabaseInfoService.class).to(EbeanDatabaseInfoService.class);

    bind(MembersService.class).to(MembersServiceImpl.class);
    bind(SubscriptionRepository.class).to(EbeanSubscriptionRepository.class);
    bind(SubscriptionMapper.class).to(SubscriptionMapperImpl.class);

    bind(AppInfoProvider.class).to(AppInfoProviderImpl.class);

    bind(AuthApi.class);
    bind(AuthApiService.class).to(AuthApiServiceImpl.class);

    bind(RoomUserSettingsRepository.class).to(EbeanRoomUserSettingsRepository.class);

    bind(UserService.class).to(UserServiceImpl.class);
    bind(UserRepository.class).to(EbeanUserRepository.class);

    bind(CapabilityService.class).to(CapabilityServiceImpl.class);

    bind(MeetingsApi.class);
    bind(MeetingsApiService.class).to(MeetingsApiServiceImpl.class);
    bind(MeetingService.class).to(MeetingServiceImpl.class);
    bind(MeetingRepository.class).to(EbeanMeetingRepository.class);
    bind(MeetingMapper.class).to(MeetingMapperImpl.class);

    bind(ParticipantService.class).to(ParticipantServiceImpl.class);
    bind(ParticipantRepository.class).to(EbeanParticipantRepository.class);
    bind(ParticipantMapper.class).to(ParticipantMapperImpl.class);

    bind(VideoServerService.class).to(VideoServerServiceImpl.class);
    bind(VideoServerMeetingRepository.class).to(EbeanVideoServerMeetingRepository.class);
    bind(VideoServerSessionRepository.class).to(EbeanVideoServerSessionRepository.class);

    bind(StoragesService.class).to(StoragesServiceImpl.class);
    bind(ProfilingService.class).to(UserManagementProfilingService.class);
    bind(AuthenticationService.class).to(UserManagementAuthenticationService.class);

    bindInterceptor(
        Matchers.any(), Matchers.annotatedWith(TimedCall.class), new TimedCallInterceptor());

    bindExceptionMapper();

    bind(VideoServerEventListener.class);
    bind(CacheHandler.class);
  }

  private void bindExceptionMapper() {
    bind(ChatsHttpExceptionHandler.class);
    bind(ClientErrorExceptionHandler.class);
    bind(JsonProcessingExceptionHandler.class);
    bind(DefaultExceptionHandler.class);
    bind(ValidationExceptionHandler.class);
  }

  @Singleton
  @Provides
  private AppConfig getAppConfig() {
    AppConfig appConfig = InfrastructureAppConfig.create().load();
    Optional.ofNullable(
            ConsulAppConfig.create(
                appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
                appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
                System.getenv("CONSUL_HTTP_TOKEN")))
        .ifPresent(consulConfig -> appConfig.add(consulConfig.load()));
    return appConfig;
  }

  @Singleton
  @Provides
  private Clock getClock() {
    return Clock.system(ZoneId.systemDefault());
  }

  @Singleton
  @Provides
  private StoragesClient getStoragesClient(AppConfig appConfig) {
    return StoragesClient.atUrl(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.STORAGES_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.STORAGES_PORT).orElseThrow()));
  }

  @Singleton
  @Provides
  private PreviewClient getPreviewClient(AppConfig appConfig) {
    return PreviewClient.atURL(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.PREVIEWER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.PREVIEWER_PORT).orElseThrow()));
  }

  @Singleton
  @Provides
  private UserManagementClient getUserManagementClient(AppConfig appConfig) {
    return UserManagementClient.atURL(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.USER_MANAGEMENT_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.USER_MANAGEMENT_PORT).orElseThrow()));
  }

  @Singleton
  @Provides
  private Database getDatabase(HikariDataSource dataSource, Clock clock) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDataSource(dataSource);
    databaseConfig.setClock(clock);
    databaseConfig.setDatabasePlatformName(Platform.POSTGRES.toString());
    return DatabaseFactory.create(databaseConfig);
  }

  @Singleton
  @Provides
  private Flyway getFlywayInstance(HikariDataSource dataSource) {
    return Flyway.configure()
        .locations("classpath:migration")
        .schemas("chats")
        .dataSource(dataSource)
        .validateMigrationNaming(true)
        .load();
  }

  @Singleton
  @Provides
  private HikariDataSource getHikariDataSource(AppConfig appConfig) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).orElseThrow());
    config.setDriverClassName(
        appConfig.get(String.class, ConfigName.DATABASE_JDBC_DRIVER).orElseThrow());
    config.setPoolName("ws-collaboration-db-pool");
    config.setUsername(
        appConfig
            .get(String.class, ConfigName.DATABASE_USERNAME)
            .orElse("carbonio-ws-collaboration-db"));
    config.setPassword(
        appConfig.get(String.class, ConfigName.DATABASE_PASSWORD).orElse("password"));
    config.setIdleTimeout(
        appConfig.get(Integer.class, ConfigName.HIKARI_IDLE_TIMEOUT).orElse(10000));
    config.setMinimumIdle(appConfig.get(Integer.class, ConfigName.HIKARI_MIN_POOL_SIZE).orElse(10));
    config.setMaximumPoolSize(
        appConfig.get(Integer.class, ConfigName.HIKARI_MAX_POOL_SIZE).orElse(10));
    config.setLeakDetectionThreshold(
        appConfig.get(Integer.class, ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD).orElse(5000));
    config.setMaxLifetime(
        appConfig.get(Integer.class, ConfigName.HIKARI_MAX_LIFETIME).orElse(600000));
    return new HikariDataSource(config);
  }

  @Singleton
  @Provides
  private MessageDispatcher getMessageDispatcher(
      AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
    return new MessageDispatcherMongooseImpl(
        httpClient,
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.XMPP_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.XMPP_SERVER_HTTP_PORT).orElseThrow()),
        Base64.getEncoder()
            .encodeToString(
                String.join(
                        ":",
                        appConfig.get(String.class, ConfigName.XMPP_SERVER_USERNAME).orElseThrow(),
                        appConfig.get(String.class, ConfigName.XMPP_SERVER_PASSWORD).orElseThrow())
                    .getBytes()),
        objectMapper);
  }

  @Provides
  private Connection getRabbitMqConnection(AppConfig appConfig) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_HOST).orElseThrow());
    factory.setPort(appConfig.get(Integer.class, ConfigName.EVENT_DISPATCHER_PORT).orElseThrow());
    factory.setUsername(
        appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_USER_USERNAME).orElseThrow());
    factory.setPassword(
        appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_USER_PASSWORD).orElseThrow());
    factory.setVirtualHost(appConfig.get(String.class, ConfigName.VIRTUAL_HOST).orElse("/"));
    factory.setRequestedHeartbeat(
        appConfig.get(Integer.class, ConfigName.REQUESTED_HEARTBEAT_IN_SEC).orElse(60));
    factory.setNetworkRecoveryInterval(
        appConfig.get(Integer.class, ConfigName.NETWORK_RECOVERY_INTERVAL_IN_MILLI).orElse(30000));
    factory.setConnectionTimeout(
        appConfig.get(Integer.class, ConfigName.CONNECTION_TIMEOUT_IN_MILLI).orElse(60000));
    factory.setAutomaticRecoveryEnabled(
        appConfig.get(Boolean.class, ConfigName.AUTOMATIC_RECOVERY_ENABLED).orElse(true));
    factory.setTopologyRecoveryEnabled(
        appConfig.get(Boolean.class, ConfigName.TOPOLOGY_RECOVERY_ENABLED).orElse(false));
    try {
      return factory.newConnection();
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Could not create connection with message broker", e);
    }
  }

  @Provides
  private Channel getRabbitMqChannel(Connection connection) {
    if (connection == null || !connection.isOpen()) {
      throw new EventDispatcherException("Message broker connection is not up!");
    }
    try {
      return connection.createChannel();
    } catch (IOException e) {
      throw new EventDispatcherException(
          "Could not create channel on message broker connection", e);
    }
  }

  @Singleton
  @Provides
  private VideoServerClient getVideoServerClient(
      AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
    return new VideoServerHttpClient(
        httpClient,
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow()),
        objectMapper);
  }

  @Singleton
  @Provides
  private VideoServerConfig getVideoServerConfig(AppConfig appConfig) {
    return new VideoServerConfigImpl(
        appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN).orElse(null));
  }
}
