// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingCreated;
import com.zextras.carbonio.async.model.Ping;
import com.zextras.carbonio.async.model.Pong;
import com.zextras.carbonio.async.model.WebsocketConnected;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.api.versioning.ChangeEvenTypeNameMigration;
import com.zextras.carbonio.chats.core.web.api.versioning.ChangeSet;
import com.zextras.carbonio.chats.core.web.api.versioning.VersionMigrationsRegistry;
import com.zextras.carbonio.chats.core.web.security.EventsWebSocketAuthenticationFilter;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketEndpointConfigurator;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import jakarta.servlet.DispatcherType;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Builder;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ApiIntegrationTest
class VersionedWebsocketIT {

  public static final String EVENTS_URL = "ws://localhost:8081/events";
  public static final String LATEST = "1.6.2";
  public static final String PREVIOUS = "1.6.0";
  public static final String DEFAULT_USER_ID = "332a9527-3388-4207-be77-6d7e2978a723";
  public static final String AUTH_TOKEN = "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL";

  private Server jettyServer;
  private EventsWebSocketManager eventsWebSocketManager;
  private final AuthenticationService authenticationService;
  private final Channel channel;
  private final ObjectMapper objectMapper;
  private final WebsocketVersionMigrator websocketVersionMigrator;
  private final ParticipantService participantService;

  public VersionedWebsocketIT(
      AuthenticationService authenticationService,
      Channel channel,
      ObjectMapper objectMapper,
      WebsocketVersionMigrator websocketVersionMigrator,
      ParticipantService participantService) {
    this.authenticationService = authenticationService;
    this.channel = channel;
    this.objectMapper = objectMapper;
    this.websocketVersionMigrator = websocketVersionMigrator;
    this.participantService = participantService;
  }

  @BeforeEach
  void setUp() throws Exception {
    jettyServer = new Server(8081);
    eventsWebSocketManager =
        new EventsWebSocketManager(
            channel, objectMapper, websocketVersionMigrator, participantService);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    jettyServer.setHandler(context);

    List<String> supportedVersions = List.of(LATEST, PREVIOUS);
    JakartaWebSocketServletContainerInitializer.configure(
        context,
        (servletContext, wsContainer) -> {
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

    jettyServer.start();

    VersionMigrationsRegistry.REGISTRY.clear();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (jettyServer != null) {
      jettyServer.stop();
      ChatsLogger.info("Test server stopped");
    }
  }

  @Test
  void meetingCreatedEventDowngraded() throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    WebSocketClient client = new WebSocketClient();
    ClientEndpointConfig clientEndpoint = buildEndpointWith(List.of(PREVIOUS));
    VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
    registry.register(
        new ChangeSet(
            new Semver(LATEST), MeetingCreated.class, List.of(new ChangeEvenTypeNameMigration())));

    try (Session ignored =
        container.connectToServer(client, clientEndpoint, URI.create(EVENTS_URL))) {
      awaitFirstMessage(client);
      declareBrokerExchange();

      dispatchEvent(
          MeetingCreated.create()
              .meetingId(UUID.randomUUID())
              .roomId(UUID.randomUUID())
              .type(EventType.MEETING_CREATED)
              .sentDate(OffsetDateTime.now()));

      shouldReceiveEventWithType(client, "MEETING_CREATED", 2);
    } finally {
      if (client.isConnected()) {
        client.disconnect();
      }
    }
  }

  @Test
  void pongEventMessageDowngraded() throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    WebSocketClient client = new WebSocketClient();
    ClientEndpointConfig clientEndpoint = buildEndpointWith(List.of(PREVIOUS));
    VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
    registry.register(
        new ChangeSet(new Semver(LATEST), Pong.class, List.of(new ChangeEvenTypeNameMigration())));

    try (Session ignored =
        container.connectToServer(client, clientEndpoint, URI.create(EVENTS_URL))) {

      awaitFirstMessage(client);

      declareBrokerExchange();
      client.sendMessage(Ping.create().type(EventType.PING).sentDate(OffsetDateTime.now()));

      shouldReceiveEventWithType(client, "pong", 2);
    } finally {
      if (client.isConnected()) {
        client.disconnect();
      }
    }
  }

  @Test
  void websocketConnectedEventDowngraded() throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    WebSocketClient client = new WebSocketClient();
    ClientEndpointConfig clientEndpoint = buildEndpointWith(List.of(PREVIOUS));
    VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
    registry.register(
        new ChangeSet(
            new Semver(LATEST),
            WebsocketConnected.class,
            List.of(new ChangeEvenTypeNameMigration())));

    try (Session ignored =
        container.connectToServer(client, clientEndpoint, URI.create(EVENTS_URL))) {

      shouldReceiveEventWithType(client, "websocketConnected", 1);
    } finally {
      if (client.isConnected()) {
        client.disconnect();
      }
    }
  }

  /**
   * This test checks our fallback sub-protocol/version ("" -> 1.6.0) in case an old client tries to
   * connect without sub-protocol negotiation.
   *
   * <p>Search also 'OLD_CLIENT_FALLBACK' in the code base
   */
  @Test
  void applyMigrationsWithFallbackNegotiation() throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    WebSocketClient client = new WebSocketClient();
    ClientEndpointConfig clientEndpoint = Builder.create().configurator(buildConfig()).build();
    VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
    registry.register(
        new ChangeSet(
            new Semver(LATEST),
            WebsocketConnected.class,
            List.of(new ChangeEvenTypeNameMigration())));

    try (Session ignored =
        container.connectToServer(client, clientEndpoint, URI.create(EVENTS_URL))) {

      shouldReceiveEventWithType(client, "websocketConnected", 1);
    } finally {
      if (client.isConnected()) {
        client.disconnect();
      }
    }
  }

  private static Stream<Arguments> semanticVersionTestList() {
    return Stream.of(
        Arguments.of(List.of("1.6.1", "1.6.2", "34.5", "1.6.0", "not_valid"), "1.6.2"),
        Arguments.of(List.of("0.5.6", "1.5.5", "1.6.1", "1.6.2"), "1.6.2"),
        Arguments.of(List.of(""), ""),
        Arguments.of(null, ""));
  }

  @ParameterizedTest
  @MethodSource("semanticVersionTestList")
  void negotiatedNewerSemanticVersionSupported(List<String> requested, String expected)
      throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    WebSocketClient client = new WebSocketClient();
    ClientEndpointConfig clientEndpoint = buildEndpointWith(requested);

    try (Session session =
        container.connectToServer(client, clientEndpoint, URI.create(EVENTS_URL))) {

      String negotiatedSubprotocol = session.getNegotiatedSubprotocol();
      assertEquals(expected, negotiatedSubprotocol);

    } finally {
      if (client.isConnected()) {
        client.disconnect();
      }
    }
  }

  /****************************************
   * Test Helpers
   ****************************************/

  private void dispatchEvent(DomainEvent domainEvent) throws IOException {
    channel.basicPublish(
        DEFAULT_USER_ID,
        "user-events",
        null,
        objectMapper.writeValueAsString(domainEvent).getBytes(StandardCharsets.UTF_8));
  }

  private void shouldReceiveEventWithType(
      WebSocketClient client, String eventType, int atEventNumber) {
    awaitDomainEventReceived(client, atEventNumber);
    assertTrue(client.getMessages().stream().anyMatch(m -> m.contains(eventType)));
  }

  private static ClientEndpointConfig buildEndpointWith(List<String> requestedVersions) {
    return Builder.create()
        .configurator(buildConfig())
        .preferredSubprotocols(requestedVersions)
        .build();
  }

  private static ClientEndpointConfig.Configurator buildConfig() {
    return new Configurator() {
      @Override
      public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Cookie", List.of(String.format("ZM_AUTH_TOKEN=%s;", AUTH_TOKEN)));
      }
    };
  }

  private void awaitFirstMessage(WebSocketClient client) {
    await().atMost(1, SECONDS).until(firstMessageReceived(client));
  }

  private void awaitDomainEventReceived(WebSocketClient client, int atEventNumber) {
    await().atMost(1, SECONDS).until(messagesReceived(client, atEventNumber));
  }

  private Callable<Boolean> firstMessageReceived(WebSocketClient client) {
    return messagesReceived(client, 1);
  }

  private Callable<Boolean> messagesReceived(WebSocketClient client, int eventNumber) {
    return () -> client.getMessages().size() == eventNumber;
  }

  private void declareBrokerExchange() throws IOException {
    channel.exchangeDeclare(DEFAULT_USER_ID, BuiltinExchangeType.DIRECT, false, false, null);
  }
}
