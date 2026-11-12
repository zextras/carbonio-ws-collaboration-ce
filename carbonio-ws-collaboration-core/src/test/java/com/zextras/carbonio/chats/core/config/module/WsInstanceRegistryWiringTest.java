// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.orbitz.consul.Consul;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.web.socket.DockerWsInstanceRegistry;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.PresenceBroadcaster;
import com.zextras.carbonio.chats.core.web.socket.WsInstanceRegistry;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceBootCleaner;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceJanitorService;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
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

  @Test
  @DisplayName("Docker mode builds a complete binding graph without any Consul binding")
  void dockerConfig_toolStageGraphHasNoConsulBinding() {
    Injector injector =
        Guice.createInjector(
            Stage.TOOL, new DockerConfig(), new PresenceDependenciesModule(), new WsModule());

    assertNull(injector.getExistingBinding(Key.get(Consul.class)));
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

  private static class WsModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(WsPresenceBootCleaner.class);
      bind(WsPresenceJanitorService.class);
    }
  }
}
