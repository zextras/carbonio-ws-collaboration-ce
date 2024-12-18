// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.web.security.EventsWebSocketAuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpoint;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import dev.resteasy.guice.GuiceResteasyBootstrapServletContextListener;
import jakarta.servlet.DispatcherType;
import jakarta.websocket.server.ServerEndpointConfig;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.flywaydb.core.Flyway;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class Boot {

  private final GuiceResteasyBootstrapServletContextListener resteasyListener;
  private final Flyway flyway;
  private final AuthenticationService authenticationService;
  private final EventsWebSocketEndpoint eventsWebSocketEndpoint;
  private final VideoServerEventListener videoServerEventListener;

  @Inject
  public Boot(
      GuiceResteasyBootstrapServletContextListener resteasyListener,
      Flyway flyway,
      AuthenticationService authenticationService,
      EventsWebSocketEndpoint eventsWebSocketEndpoint,
      VideoServerEventListener videoServerEventListener) {
    this.resteasyListener = resteasyListener;
    this.flyway = flyway;
    this.authenticationService = authenticationService;
    this.eventsWebSocketEndpoint = eventsWebSocketEndpoint;
    this.videoServerEventListener = videoServerEventListener;
  }

  public void boot() throws Exception {
    flyway.migrate();

    Server server =
        new Server(new InetSocketAddress(ChatsConstant.SERVER_HOST, ChatsConstant.SERVER_PORT));
    ContextHandlerCollection handlers = new ContextHandlerCollection();

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

    JakartaWebSocketServletContainerInitializer.configure(
        context,
        (servletContext, wsContainer) -> {
          wsContainer.setDefaultMaxSessionIdleTimeout(0L);
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

    context.addEventListener(resteasyListener);
    context.addServlet(new ServletHolder(HttpServletDispatcher.class), "/*");

    videoServerEventListener.start();

    handlers.addHandler(context);
    server.setHandler(handlers);
    server.start();
    server.join();
  }
}
