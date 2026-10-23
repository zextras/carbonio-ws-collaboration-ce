// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.security.EventsWebSocketAuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.SessionPingManager;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
import com.zextras.carbonio.chats.core.web.socket.action.WsActionRouter;
import com.zextras.carbonio.chats.core.web.socket.impl.EventsWebSocketManagerImpl;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ConsulMockServer;
import jakarta.servlet.DispatcherType;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class EventsWebSocketCloseIT {

  private static final String EVENTS_URL = "ws://localhost:8082/events";
  private static final String USER_ID = "332a9527-3388-4207-be77-6d7e2978a723";
  private static final String AUTH_TOKEN = "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL";

  private final AuthenticationService authenticationService;
  private final Connection connection;
  private final RabbitConnectionPoolService rabbitPool;
  private final ObjectMapper objectMapper;
  private final WebsocketVersionMigrator websocketVersionMigrator;
  private final ParticipantService participantService;
  private final CacheVideoServerSession cacheVideoServerSession;
  private final WsActionRouter wsActionRouter;
  private final WsPresenceManager wsPresenceManager;
  private final WsPresenceRepository wsPresenceRepository;

  private Server jettyServer;

  public EventsWebSocketCloseIT(
      AuthenticationService authenticationService,
      Connection connection,
      RabbitConnectionPoolService rabbitPool,
      ObjectMapper objectMapper,
      WebsocketVersionMigrator websocketVersionMigrator,
      ParticipantService participantService,
      CacheVideoServerSession cacheVideoServerSession,
      WsActionRouter wsActionRouter,
      WsPresenceManager wsPresenceManager,
      WsPresenceRepository wsPresenceRepository) {
    this.authenticationService = authenticationService;
    this.connection = connection;
    this.rabbitPool = rabbitPool;
    this.objectMapper = objectMapper;
    this.websocketVersionMigrator = websocketVersionMigrator;
    this.participantService = participantService;
    this.cacheVideoServerSession = cacheVideoServerSession;
    this.wsActionRouter = wsActionRouter;
    this.wsPresenceManager = wsPresenceManager;
    this.wsPresenceRepository = wsPresenceRepository;
  }

  @BeforeAll
  static void setUpConsul(ConsulMockServer consulMockServer) {
    consulMockServer.mockServiceIdResponse(UUID.randomUUID());
  }

  @AfterEach
  void stopServer() throws Exception {
    if (jettyServer != null) {
      jettyServer.stop();
    }
  }

  @Test
  @DisplayName("Broker session is torn down even if the session-disconnected hook fails")
  void close_disconnectHookFails_stillTearsDownBrokerSession() throws Exception {
    startServer(
        new FailingDisconnectHookManager(
            rabbitPool,
            objectMapper,
            websocketVersionMigrator,
            cacheVideoServerSession,
            participantService,
            wsActionRouter,
            wsPresenceManager,
            new EventWebSocketSessions(new SessionPingManager())));
    WebSocketClient client = new WebSocketClient();
    Session session = connect(client);
    String queueId = awaitQueueId(client);
    await().atMost(2, SECONDS).until(() -> queueExists(queueId));

    session.close();

    await().atMost(5, SECONDS).until(() -> !queueExists(queueId));
  }

  @Test
  @DisplayName("A WebSocket error cleans up the broker session and the presence row")
  void error_cleansUpBrokerSessionAndPresence() throws Exception {
    startServer(
        new EventsWebSocketManagerImpl(
            rabbitPool,
            objectMapper,
            websocketVersionMigrator,
            cacheVideoServerSession,
            participantService,
            wsActionRouter,
            wsPresenceManager,
            new EventWebSocketSessions(new SessionPingManager())));
    WebSocketClient client = new WebSocketClient();
    Session session = connect(client);
    String queueId = awaitQueueId(client);
    await().atMost(2, SECONDS).until(() -> wsPresenceRepository.countOnlineByUserId(USER_ID) == 1);

    session.getBasicRemote().sendText("x".repeat(200_000));

    await().atMost(5, SECONDS).until(() -> !queueExists(queueId));
    await().atMost(5, SECONDS).until(() -> wsPresenceRepository.countOnlineByUserId(USER_ID) == 0);
  }

  private boolean queueExists(String queueId) {
    try (Channel channel = connection.createChannel()) {
      channel.queueDeclarePassive(queueId);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private String awaitQueueId(WebSocketClient client) throws Exception {
    await().atMost(2, SECONDS).until(() -> !client.getMessages().isEmpty());
    return objectMapper.readTree(client.getMessages().get(0)).get("queueId").asText();
  }

  private Session connect(WebSocketClient client) throws Exception {
    ClientEndpointConfig config =
        ClientEndpointConfig.Builder.create()
            .configurator(
                new ClientEndpointConfig.Configurator() {
                  @Override
                  public void beforeRequest(Map<String, List<String>> headers) {
                    headers.put("Cookie", List.of("ZM_AUTH_TOKEN=%s;".formatted(AUTH_TOKEN)));
                  }
                })
            .build();
    return ContainerProvider.getWebSocketContainer()
        .connectToServer(client, config, URI.create(EVENTS_URL));
  }

  private void startServer(EventsWebSocketManager manager) throws Exception {
    jettyServer = new Server(8082);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    jettyServer.setHandler(context);
    JakartaWebSocketServletContainerInitializer.configure(
        context,
        (servletContext, wsContainer) -> {
          wsContainer.addEndpoint(
              jakarta.websocket.server.ServerEndpointConfig.Builder.create(
                      manager.getClass(), "/events")
                  .configurator(new EventsWebSocketEndpointConfigurator(manager))
                  .build());
          servletContext
              .addFilter(
                  "eventsWebSocketAuthenticationFilter",
                  EventsWebSocketAuthenticationFilter.create(authenticationService))
              .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/events");
        });
    jettyServer.start();
  }

  @ServerEndpoint(value = "/events")
  public static class FailingDisconnectHookManager extends EventsWebSocketManagerImpl {

    public FailingDisconnectHookManager(
        RabbitConnectionPoolService rabbitPool,
        ObjectMapper objectMapper,
        WebsocketVersionMigrator migrator,
        CacheVideoServerSession cacheVideoServerSession,
        ParticipantService participantService,
        WsActionRouter wsActionRouter,
        WsPresenceManager wsPresenceManager,
        EventWebSocketSessions sessions) {
      super(
          rabbitPool,
          objectMapper,
          migrator,
          cacheVideoServerSession,
          participantService,
          wsActionRouter,
          wsPresenceManager,
          sessions);
    }

    @Override
    protected void onSessionDisconnected(UUID queueId) {
      throw new RuntimeException("DB down");
    }
  }
}
