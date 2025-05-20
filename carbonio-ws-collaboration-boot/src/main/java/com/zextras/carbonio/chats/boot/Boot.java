// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import com.google.inject.Inject;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.security.EventsWebSocketAuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import dev.resteasy.guice.GuiceResteasyBootstrapServletContextListener;
import jakarta.servlet.DispatcherType;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.EnumSet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final GuiceResteasyBootstrapServletContextListener resteasyListener;
  private final Flyway flyway;
  private final HikariDataSource hikariDataSource;
  private final AuthenticationService authenticationService;
  private final EventsWebSocketManager eventsWebSocketManager;
  private final VideoServerEventListener videoServerEventListener;

  @Inject
  public Boot(
      GuiceResteasyBootstrapServletContextListener resteasyListener,
      Flyway flyway,
      HikariDataSource hikariDataSource,
      AuthenticationService authenticationService,
      EventsWebSocketManager eventsWebSocketManager,
      VideoServerEventListener videoServerEventListener) {
    this.resteasyListener = resteasyListener;
    this.flyway = flyway;
    this.hikariDataSource = hikariDataSource;
    this.authenticationService = authenticationService;
    this.eventsWebSocketManager = eventsWebSocketManager;
    this.videoServerEventListener = videoServerEventListener;
  }

  public void boot() throws Exception {
    flyway.migrate();

    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setHost(ChatsConstant.SERVER_HOST);
    connector.setPort(ChatsConstant.SERVER_PORT);
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);

    JakartaWebSocketServletContainerInitializer.configure(
        context,
        (servletContext, wsContainer) -> {
          wsContainer.setDefaultMaxSessionIdleTimeout(30_000L);
          wsContainer.setDefaultMaxTextMessageBufferSize(65536);
          wsContainer.setDefaultMaxBinaryMessageBufferSize(65536);

          wsContainer.addEndpoint(
              ServerEndpointConfig.Builder.create(EventsWebSocketManager.class, "/events")
                  .configurator(new EventsWebSocketEndpointConfigurator(eventsWebSocketManager))
                  .build());
          servletContext
              .addFilter(
                  "eventsWebSocketAuthenticationFilter",
                  EventsWebSocketAuthenticationFilter.create(authenticationService))
              .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/events");
        });

    context.addEventListener(resteasyListener);
    context.addServlet(new ServletHolder(HttpServletDispatcher.class), "/*");

    videoServerEventListener.start();

    server.start();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    hikariDataSource.close();
                    ChatsLogger.info("Shutting down server...");
                    server.stop();
                  } catch (Exception e) {
                    throw new InternalErrorException(e);
                  }
                }));

    server.join();
  }
}
