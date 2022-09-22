// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.websocket.ChatsEndpoint;
import java.net.InetSocketAddress;
import java.util.Optional;
import javax.inject.Inject;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final AppConfig                                    appConfig;
  private final GuiceResteasyBootstrapServletContextListener resteasyListener;
  private final Flyway                                       flyway;
  private final Connection                                   eventDispatcherConnection;
  private final AuthenticationService                        authenticationService;
  private final ObjectMapper                                 objectMapper;

  @Inject
  public Boot(
    AppConfig appConfig,
    GuiceResteasyBootstrapServletContextListener resteasyListener,
    Flyway flyway,
    Optional<Connection> eventDispatcherConnection,
    AuthenticationService authenticationService,
    ObjectMapper objectMapper
  ) {
    this.appConfig = appConfig;
    this.resteasyListener = resteasyListener;
    this.flyway = flyway;
    this.eventDispatcherConnection = eventDispatcherConnection.orElse(null);
    this.authenticationService = authenticationService;
    this.objectMapper = objectMapper;
  }

  public void boot() throws Exception {
    flyway.migrate();

    if (appConfig.getEnvType().equals(EnvironmentType.DEVELOPMENT)) {
      ChatsLogger.warn("****** RUNNING IN DEVELOPMENT MODE! DO NOT USE IN PRODUCTION ENVIRONMENTS ******");
    }
    Server server = new Server(new InetSocketAddress(ChatsConstant.SERVER_HOST, ChatsConstant.SERVER_PORT));
    ContextHandlerCollection handlers = new ContextHandlerCollection();
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    if (eventDispatcherConnection != null) {
      JavaxWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
        wsContainer.setDefaultMaxTextMessageBufferSize(65535);
        wsContainer.setAsyncSendTimeout(-1);
        wsContainer.addEndpoint(ServerEndpointConfig.Builder
          .create(ChatsEndpoint.class, "/events")
          .configurator(new EventWebSocketEndpointConfigurator(eventDispatcherConnection, authenticationService,
            objectMapper))
          .build());
      });
    }
    context.addEventListener(resteasyListener);
    context.addServlet(new ServletHolder(HttpServletDispatcher.class), "/*");
    handlers.addHandler(context);
    server.setHandler(handlers);
    server.start();
    server.join();
  }

  public static class EventWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

    private final Connection            connection;
    private final AuthenticationService authenticationService;
    private final ObjectMapper          objectMapper;

    public EventWebSocketEndpointConfigurator(
      Connection connection,
      AuthenticationService authenticationService,
      ObjectMapper objectMapper
    ) {
      this.connection = connection;
      this.authenticationService = authenticationService;
      this.objectMapper = objectMapper;
    }

    public <T> T getEndpointInstance(Class<T> clazz) {
      return (T) new ChatsEndpoint(connection, authenticationService, objectMapper);
    }
  }
}
