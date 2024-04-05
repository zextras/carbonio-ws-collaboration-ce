// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.security.EventsWebSocketAuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpoint;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import jakarta.inject.Inject;
import jakarta.servlet.DispatcherType;
import jakarta.websocket.server.ServerEndpointConfig;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Optional;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final GuiceResteasyBootstrapServletContextListener resteasyListener;
  private final Flyway flyway;
  private final Connection eventDispatcherConnection;
  private final AuthenticationService authenticationService;
  private final EventsWebSocketEndpoint eventsWebSocketEndpoint;
  private final VideoServerEventListener videoServerEventListener;

  @Inject
  public Boot(
      GuiceResteasyBootstrapServletContextListener resteasyListener,
      Flyway flyway,
      Optional<Connection> eventDispatcherConnection,
      AuthenticationService authenticationService,
      EventsWebSocketEndpoint eventsWebSocketEndpoint,
      VideoServerEventListener videoServerEventListener) {
    this.resteasyListener = resteasyListener;
    this.flyway = flyway;
    this.eventDispatcherConnection = eventDispatcherConnection.orElse(null);
    this.authenticationService = authenticationService;
    this.eventsWebSocketEndpoint = eventsWebSocketEndpoint;
    this.videoServerEventListener = videoServerEventListener;
  }

  public void boot() throws Exception {
    flyway.migrate();

    Server server =
        new Server(new InetSocketAddress(ChatsConstant.SERVER_HOST, ChatsConstant.SERVER_PORT));
    ContextHandlerCollection handlers = new ContextHandlerCollection();

    org.eclipse.jetty.servlet.ServletContextHandler wsContext =
        new org.eclipse.jetty.servlet.ServletContextHandler(
            org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS);

    if (eventDispatcherConnection != null) {
      JakartaWebSocketServletContainerInitializer.configure(
          wsContext,
          (servletContext, wsContainer) -> {
            wsContainer.setDefaultMaxTextMessageBufferSize(65535);
            wsContainer.addEndpoint(
                ServerEndpointConfig.Builder.create(EventsWebSocketEndpoint.class, "/events")
                    .configurator(new EventsWebSocketEndpointConfigurator(eventsWebSocketEndpoint))
                    .build());
            servletContext
                .addFilter(
                    "eventsWebSocketAuthenticationFilter",
                    EventsWebSocketAuthenticationFilter.create(authenticationService))
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/events");
          });
      Recoverable recoverableConnection = (Recoverable) eventDispatcherConnection;
      recoverableConnection.addRecoveryListener(
          new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
              ChatsLogger.warn("VideoServer events connection recovery finished successfully");
              videoServerEventListener.start();
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
              ChatsLogger.warn("VideoServer events connection recovery started...");
            }
          });
    }

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.addEventListener(resteasyListener);
    context.addServlet(new ServletHolder(HttpServletDispatcher.class), "/*");

    videoServerEventListener.start();

    handlers.addHandler(context);
    handlers.addHandler(wsContext);
    server.setHandler(handlers);
    server.start();
    server.join();
  }
}
