// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import com.google.inject.Inject;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.ServerConfiguration;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.security.EventsWebSocketAuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import dev.resteasy.guice.GuiceResteasyBootstrapServletContextListener;
import jakarta.servlet.DispatcherType;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final ServerConfiguration serverConfiguration;
  private final GuiceResteasyBootstrapServletContextListener resteasyListener;
  private final Flyway flyway;
  private final HikariDataSource hikariDataSource;
  private final AuthenticationService authenticationService;
  private final EventsWebSocketManager eventsWebSocketManager;
  private final VideoServerEventListener videoServerEventListener;
  private final AppConfig appConfig;

  @Inject
  public Boot(
      ServerConfiguration serverConfiguration,
      GuiceResteasyBootstrapServletContextListener resteasyListener,
      Flyway flyway,
      HikariDataSource hikariDataSource,
      AuthenticationService authenticationService,
      EventsWebSocketManager eventsWebSocketManager,
      VideoServerEventListener videoServerEventListener,
      AppConfig appConfig) {
    this.serverConfiguration = serverConfiguration;
    this.resteasyListener = resteasyListener;
    this.flyway = flyway;
    this.hikariDataSource = hikariDataSource;
    this.authenticationService = authenticationService;
    this.eventsWebSocketManager = eventsWebSocketManager;
    this.videoServerEventListener = videoServerEventListener;
    this.appConfig = appConfig;
  }

  public void boot() throws Exception {
    ChatsLogger.info("Application latest version: " + VersionProvider.getVersion());
    ChatsLogger.info("Supported versions: " + VersionProvider.getSupportedVersions());

    flyway.migrate();

    Server server = new Server(createThreadPool());
    ServerConnector connector = new ServerConnector(server);
    connector.setHost(serverConfiguration.getHost());
    connector.setPort(serverConfiguration.getPort());
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

          List<String> supportedVersions = VersionProvider.getSupportedVersions();
          wsContainer.addEndpoint(
              ServerEndpointConfig.Builder.create(EventsWebSocketManager.class, "/events")
                  .configurator(new EventsWebSocketEndpointConfigurator(eventsWebSocketManager))
                  .subprotocols(supportedVersions)
                  .build());
          servletContext
              .addFilter(
                  "eventsWebSocketAuthenticationFilter",
                  EventsWebSocketAuthenticationFilter.create(authenticationService))
              .addMappingForUrlPatterns(
                  EnumSet.of(DispatcherType.REQUEST),
                  false /* It's applied before other filters */,
                  "/events");
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

  private ThreadPool createThreadPool() {
    Integer maxThreads = appConfig.get(Integer.class, ConfigName.MAX_THREADS).orElse(2048);
    Integer minThreads = appConfig.get(Integer.class, ConfigName.MIN_THREADS).orElse(8);
    Integer maxQueuedRequests =
        appConfig.get(Integer.class, ConfigName.MAX_QUEUE_REQUESTS).orElse(2048);

    final BlockingQueue<Runnable> queue =
        new BlockingArrayQueue<>(minThreads, maxThreads, maxQueuedRequests);
    final QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, queue);
    threadPool.setName("WSC-Jetty-ThreadPool");
    return threadPool;
  }
}
