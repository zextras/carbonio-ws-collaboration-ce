// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.zextras.carbonio.chats.core.web.api.versioning.ChangeEventTypeNameMigration;
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
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EncodeException;
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
import org.eclipse.jetty.websocket.core.exception.UpgradeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ApiIntegrationTest
class VersionedWebsocketIT {

  public static final String EVENTS_URL = "ws://localhost:8081/events";
  public static final String LATEST = "1.6.2";
  public static final String OLDEST = "1.6.0";
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

  @AfterEach
  void init() {
    VersionMigrationsRegistry.REGISTRY.clear();
  }

  @Test
  void meetingCreatedEventDowngraded() throws Exception {
    withWebSocketServer(
        List.of(LATEST, OLDEST),
        () -> {
          var clientContextWithVersion = new ClientContextWithVersion(List.of(OLDEST));
          var client = clientContextWithVersion.client();
          VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
          registry.register(
              new ChangeSet(
                  new Semver(LATEST),
                  MeetingCreated.class,
                  List.of(new ChangeEventTypeNameMigration())));

          try (Session ignored = createSession(clientContextWithVersion)) {
            awaitFirstMessage(client);
            declareBrokerExchange();
            dispatchEvent(
                MeetingCreated.create()
                    .meetingId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .type(EventType.MEETING_CREATED)
                    .sentDate(OffsetDateTime.now()));

            shouldReceiveEventWithType(client, "MEETING_CREATED", 2);

          } catch (IOException e) {
            throw new RuntimeException(e);
          } finally {
            tryDisconnect(client);
          }
        });
  }

  @Test
  void pongEventMessageDowngraded() throws Exception {
    withWebSocketServer(
        List.of(LATEST, OLDEST),
        () -> {
          var clientContext = new ClientContextWithVersion(List.of(OLDEST));
          var client = clientContext.client();
          VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
          registry.register(
              new ChangeSet(
                  new Semver(LATEST), Pong.class, List.of(new ChangeEventTypeNameMigration())));

          try (Session ignored = createSession(clientContext)) {

            awaitFirstMessage(client);
            declareBrokerExchange();
            client.sendMessage(Ping.create().type(EventType.PING).sentDate(OffsetDateTime.now()));

            shouldReceiveEventWithType(client, "pong", 2);

          } catch (IOException | EncodeException e) {
            throw new RuntimeException(e);
          } finally {
            tryDisconnect(client);
          }
        });
  }

  @Test
  void websocketConnectedEventDowngraded() throws Exception {
    withWebSocketServer(
        List.of(LATEST, OLDEST),
        () -> {
          var clientContext = new ClientContextWithVersion(List.of(OLDEST));
          VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
          registry.register(
              new ChangeSet(
                  new Semver(LATEST),
                  WebsocketConnected.class,
                  List.of(new ChangeEventTypeNameMigration())));

          try (Session ignored = createSession(clientContext)) {

            shouldReceiveEventWithType(clientContext.client(), "websocketConnected", 1);

          } catch (IOException e) {
            throw new RuntimeException(e);
          } finally {
            tryDisconnect(clientContext.client());
          }
        });
  }

  /**
   * This test checks our fallback sub-protocol/version ("" -> 1.6.0) in case an old client tries to
   * connect without sub-protocol negotiation.
   *
   * <p>Search also 'OLD_CLIENT_FALLBACK' in the code base
   */
  @Test
  void applyMigrationsWithFallbackNegotiation() throws Exception {
    withWebSocketServer(
        List.of(LATEST, OLDEST),
        () -> {
          var clientContextWithoutVersion = new ClientContextWithoutVersion();
          var client = clientContextWithoutVersion.client();
          VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;
          registry.register(
              new ChangeSet(
                  new Semver(LATEST),
                  WebsocketConnected.class,
                  List.of(new ChangeEventTypeNameMigration())));

          try (Session ignored = createSession(clientContextWithoutVersion)) {

            shouldReceiveEventWithType(client, "websocketConnected", 1);

          } catch (IOException e) {
            throw new RuntimeException(e);
          } finally {
            tryDisconnect(client);
          }
        });
  }

  private static Stream<Arguments> validNegotiationScenarios() {
    return Stream.of(
        Arguments.of(new LatestVersion()),
        Arguments.of(new LatestValidVersion()),
        Arguments.of(new EmptyVersionFallback()));
  }

  @ParameterizedTest
  @MethodSource("validNegotiationScenarios")
  void validNegotiationScenarios(VersionNegotiationScenario scenario) throws Exception {
    withWebSocketServer(
        scenario.serverStatus().supportedVersions(),
        () -> {
          List<String> previous = scenario.clientStatus().clientVersions();
          var clientContext =
              previous != null
                  ? new ClientContextWithVersion(previous)
                  : new ClientContextWithoutVersion();
          try (Session session = createSession(clientContext)) {

            awaitFirstMessage(clientContext.client());
            String negotiatedSubprotocol = session.getNegotiatedSubprotocol();
            assertEquals(scenario.expectedResponseVersion(), negotiatedSubprotocol);

          } catch (IOException e) {
            throw new RuntimeException(e);
          } finally {
            tryDisconnect(clientContext.client());
          }
        });
  }

  private static Stream<Arguments> failureNegotiationScenarios() {
    return Stream.of(
        Arguments.of(new ServerWithoutSupportedVersions()), Arguments.of(new NoMatchingVersions()));
  }

  @ParameterizedTest
  @MethodSource("failureNegotiationScenarios")
  void failureNegotiationScenarios(VersionNegotiationScenario scenario) throws Exception {
    withWebSocketServer(
        scenario.serverStatus().supportedVersions(),
        () -> {
          List<String> previous = scenario.clientStatus().clientVersions();
          var clientContext =
              previous != null
                  ? new ClientContextWithVersion(previous)
                  : new ClientContextWithoutVersion();
          try {
            IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> createSession(clientContext));
            // Assert on the root cause to make the test more specific
            assertInstanceOf(UpgradeException.class, getRootCause(exception));

          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            tryDisconnect(clientContext.client());
          }
        });
  }

  /****************************************
   * Header Version Scenarios
   ****************************************/

  private interface VersionNegotiationScenario {
    ServerStatus serverStatus();

    ClientStatus clientStatus();

    String expectedResponseVersion();
  }

  private record ServerStatus(List<String> supportedVersions) {}

  private record ClientStatus(List<String> clientVersions) {}

  private record LatestVersion(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements VersionNegotiationScenario {
    public LatestVersion() {
      this(
          new ServerStatus(List.of("1.6.2", "1.6.1", "1.6.0")),
          new ClientStatus(List.of("1.6.2", "1.6.1", "1.6.0")),
          "1.6.2");
    }
  }

  private record LatestValidVersion(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements VersionNegotiationScenario {
    public LatestValidVersion() {
      this(
          new ServerStatus(List.of("1.6.2", "1.6.1", "1.6.0")),
          new ClientStatus(List.of("1.6.1", "1.6.2", "34.5", "1.6.0", "not_valid")),
          "1.6.2");
    }
  }

  private record EmptyVersionFallback(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements VersionNegotiationScenario {
    public EmptyVersionFallback() {
      this(new ServerStatus(List.of("1.6.2", "1.6.1", "1.6.0")), new ClientStatus(null), "");
    }
  }

  private record ServerWithoutSupportedVersions(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements VersionNegotiationScenario {
    public ServerWithoutSupportedVersions() {
      this(new ServerStatus(null), new ClientStatus(List.of("1.6.2", "1.6.1", "1.6.0")), "");
    }
  }

  private record NoMatchingVersions(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements VersionNegotiationScenario {
    public NoMatchingVersions() {
      this(
          new ServerStatus(List.of("1.6.2", "1.6.1", "1.6.0")),
          new ClientStatus(List.of("7.1.0", "8.0.0")),
          "");
    }
  }

  /****************************************
   * Object Helpers
   ****************************************/

  interface ClientContext {
    WebSocketContainer container();

    ClientEndpointConfig endpointConfig();

    WebSocketClient client();
  }

  record ClientContextWithVersion(
      WebSocketContainer container, ClientEndpointConfig endpointConfig, WebSocketClient client)
      implements ClientContext {
    ClientContextWithVersion(List<String> previous) {
      this(
          ContainerProvider.getWebSocketContainer(),
          buildEndpointWith(previous),
          new WebSocketClient());
    }
  }

  record ClientContextWithoutVersion(
      WebSocketContainer container, ClientEndpointConfig endpointConfig, WebSocketClient client)
      implements ClientContext {
    ClientContextWithoutVersion() {
      this(
          ContainerProvider.getWebSocketContainer(),
          Builder.create().configurator(buildConfig()).build(),
          new WebSocketClient());
    }
  }

  /****************************************
   * Utility Functions
   ****************************************/

  private static Session createSession(ClientContext clientContext) {
    try {
      Session session =
          clientContext
              .container()
              .connectToServer(
                  clientContext.client(), clientContext.endpointConfig(), URI.create(EVENTS_URL));
      Assertions.assertNotNull(session);
      return session;
    } catch (DeploymentException | IOException e) {
      ChatsLogger.error("Error connecting to WebSocket server", e);
      throw new IllegalStateException(e);
    }
  }

  private static void tryDisconnect(WebSocketClient client) {
    if (client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException e) {
        ChatsLogger.error("Disconnect failed", e);
      }
    }
  }

  private void dispatchEvent(DomainEvent domainEvent) throws IOException {
    channel.basicPublish(
        DEFAULT_USER_ID,
        "user-events",
        null,
        objectMapper.writeValueAsString(domainEvent).getBytes(StandardCharsets.UTF_8));
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

  private void shouldReceiveEventWithType(
      WebSocketClient client, String eventType, int atEventNumber) {
    awaitDomainEventReceived(client, atEventNumber);
    assertTrue(client.getMessages().stream().anyMatch(m -> m.contains(eventType)));
  }

  private void startWebSocketServer(List<String> serverSupportedVersions) throws Exception {
    jettyServer = new Server(8081);
    eventsWebSocketManager =
        new EventsWebSocketManager(
            channel, objectMapper, websocketVersionMigrator, participantService);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    jettyServer.setHandler(context);

    JakartaWebSocketServletContainerInitializer.configure(
        context,
        (servletContext, wsContainer) -> {
          wsContainer.addEndpoint(
              ServerEndpointConfig.Builder.create(EventsWebSocketManager.class, "/events")
                  .configurator(new EventsWebSocketEndpointConfigurator(eventsWebSocketManager))
                  .subprotocols(serverSupportedVersions)
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
  }

  private void stopWebSocketServer() throws Exception {
    if (jettyServer != null) {
      jettyServer.stop();
      ChatsLogger.info("Test server stopped");
    }
  }

  private void withWebSocketServer(List<String> supportedVersions, Runnable runnable)
      throws Exception {
    try {
      startWebSocketServer(supportedVersions);
      runnable.run();
    } finally {
      stopWebSocketServer();
    }
  }

  private static Throwable getRootCause(IllegalStateException exception) {
    return exception.getCause().getCause();
  }
}
