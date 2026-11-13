// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.orbitz.consul.Consul;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentialReader;
import com.zextras.carbonio.chats.core.config.impl.AppConfigCredentialReader;
import com.zextras.carbonio.chats.core.config.impl.ConsulCredentialReader;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.web.socket.DockerWsInstanceRegistry;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.PresenceBroadcaster;
import com.zextras.carbonio.chats.core.web.socket.WsInstanceRegistry;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceBootCleaner;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceJanitorService;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WsInstanceRegistryWiringTest {

  private static class PresenceDependenciesModule extends AbstractModule {
    @Override
    protected void configure() {
      Multibinder.newSetBinder(binder(), ConfigContribution.class);
      bind(WsPresenceRepository.class).toInstance(mock(WsPresenceRepository.class));
      bind(PresenceBroadcaster.class).toInstance(mock(PresenceBroadcaster.class));
      bind(WsPresenceManager.class).toInstance(mock(WsPresenceManager.class));
      bind(EventWebSocketSessions.class).toInstance(mock(EventWebSocketSessions.class));
    }
  }

  private static void assertGraphWithoutConsul(AbstractModule config) {
    Injector injector =
        Guice.createInjector(Stage.TOOL, config, new CoreModule(), new ExtensionModule());

    assertNull(injector.getExistingBinding(Key.get(Consul.class)));
    assertNotNull(injector.getBinding(WsPresenceJanitorService.class));
    assertNotNull(injector.getBinding(WsPresenceBootCleaner.class));
    assertNotNull(injector.getBinding(WsInstanceRegistry.class));
  }

  @Test
  @DisplayName("Docker mode builds the full CE graph without any Consul binding")
  void dockerConfig_fullGraphHasNoConsulBinding() {
    assertGraphWithoutConsul(new DockerConfig());
  }

  @Test
  @DisplayName("Production mode builds the full CE graph without any Consul binding")
  void productionConfig_fullGraphHasNoConsulBinding() {
    assertGraphWithoutConsul(new ProductionConfig());
  }

  @Test
  @DisplayName("Docker mode resolves the registry and the presence classes")
  void dockerConfig_resolvesPresenceClasses() {
    Injector injector =
        Guice.createInjector(
            Stage.DEVELOPMENT,
            new DockerConfig(),
            new PresenceDependenciesModule(),
            new WsModule());

    assertInstanceOf(
        DockerWsInstanceRegistry.class, injector.getInstance(WsInstanceRegistry.class));
    assertNotNull(injector.getInstance(WsPresenceJanitorService.class));
    assertNotNull(injector.getInstance(WsPresenceBootCleaner.class));
    assertNull(injector.getExistingBinding(Key.get(Consul.class)));
  }

  private static Injector credentialReaderInjector(AbstractModule config, AppConfig appConfig) {
    return Guice.createInjector(
        Stage.DEVELOPMENT,
        Modules.override(config)
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(AppConfig.class).toInstance(appConfig);
                  }
                }),
        new PresenceDependenciesModule());
  }

  private static AppConfig consulAppConfig() {
    AppConfig appConfig = mock(AppConfig.class);
    when(appConfig.get(String.class, ConfigName.CONSUL_HOST)).thenReturn(Optional.of("localhost"));
    when(appConfig.get(Integer.class, ConfigName.CONSUL_PORT)).thenReturn(Optional.of(8500));
    return appConfig;
  }

  @Test
  @DisplayName("Production mode reads the message dispatcher credentials from Consul lazily")
  void productionConfig_credentialReaderIsConsul() {
    AppConfig appConfig = consulAppConfig();

    Injector injector = credentialReaderInjector(new ProductionConfig(), appConfig);

    assertInstanceOf(
        ConsulCredentialReader.class,
        injector.getInstance(MessageDispatcherCredentialReader.class));
  }

  @Test
  @DisplayName("Docker mode reads the message dispatcher credentials from the app config")
  void dockerConfig_credentialReaderIsAppConfig() {
    AppConfig appConfig = mock(AppConfig.class);

    Injector injector = credentialReaderInjector(new DockerConfig(), appConfig);

    assertInstanceOf(
        AppConfigCredentialReader.class,
        injector.getInstance(MessageDispatcherCredentialReader.class));
    verifyNoInteractions(appConfig);
  }

  private static class WsModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(WsPresenceBootCleaner.class);
      bind(WsPresenceJanitorService.class);
    }
  }
}
