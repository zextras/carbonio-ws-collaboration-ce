// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.Set;

/**
 * Registers the CE {@link EventsWebSocketManager} at {@code /events} as CE Boot did on Jetty: one
 * shared manager instance (the CDI bean), the CE subprotocol negotiation and the supported API
 * versions from {@link VersionProvider} as subprotocols.
 */
public class EventsWebSocketApplicationConfig implements ServerApplicationConfig {

  public static final String PATH = "/events";

  @Override
  public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned) {
    return Set.of(
        ServerEndpointConfig.Builder.create(EventsWebSocketManager.class, PATH)
            .configurator(new EventsWebSocketEndpointConfigurator())
            .subprotocols(VersionProvider.getSupportedVersions())
            .build());
  }

  @Override
  public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
    return Set.of();
  }
}
