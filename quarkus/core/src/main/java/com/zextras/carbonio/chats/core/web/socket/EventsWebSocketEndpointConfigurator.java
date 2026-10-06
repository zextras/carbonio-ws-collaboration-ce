// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.zextras.carbonio.chats.core.web.security.AuthenticationFilter;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import io.quarkus.arc.Arc;
import io.quarkus.websockets.client.runtime.WebSocketPrincipal;
import jakarta.websocket.Session;
import java.util.UUID;
import java.util.function.Supplier;

import com.vdurmont.semver4j.Semver;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EventsWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

  public static final String USER_ID = "userId";
  public static final String QUEUE_ID = "queueId";

  private final Supplier<EventsWebSocketManager> eventsWebSocketManager;

  /** Resolves the manager bean lazily (the container builds the config outside CDI). */
  public EventsWebSocketEndpointConfigurator() {
    this(() -> Arc.container().instance(EventsWebSocketManager.class).get());
  }

  public EventsWebSocketEndpointConfigurator(EventsWebSocketManager eventsWebSocketManager) {
    this(() -> eventsWebSocketManager);
  }

  private EventsWebSocketEndpointConfigurator(Supplier<EventsWebSocketManager> manager) {
    this.eventsWebSocketManager = manager;
  }

  /**
   * The queue id of a websocket session. Jetty session ids were UUIDs and CE uses them as queue
   * ids; Undertow ids are not, so a UUID is assigned once per session (session user properties are
   * per session).
   */
  public static String queueIdOf(Session session) {
    return (String)
        session.getUserProperties().computeIfAbsent(QUEUE_ID, k -> UUID.randomUUID().toString());
  }

  /**
   * The user id of a websocket session, from the identity authenticated on the upgrade request
   * (see {@link EventsWebSocketAuthenticationFilter}); the {@link #USER_ID} user property is a fallback.
   */
  public static String userIdOf(Session session) {
    if (session.getUserPrincipal() instanceof WebSocketPrincipal principal) {
      Optional<String> id =
          AuthenticationFilter.toPrincipal(principal.getSecurityIdentity(), null)
              .map(UserPrincipal::getId);
      if (id.isPresent()) {
        return id.get();
      }
    }
    return (String) session.getUserProperties().get(USER_ID);
  }

  @Override
  public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
    /**
     * OLD_CLIENT_FALLBACK if the version is not present, it's set to empty string for old client
     * compatibility
     *
     * <p>Due to websocket spec, we cannot have a direct fallback to oldest version because there is
     * no match with requested from client so we return empty string. Then ws manager we will handle
     * it as the oldest supported version, so it will perform migrations.
     */
    if (requested == null || requested.isEmpty()) {
      return "";
    }

    // We need to be sure that the chosen semantic version is the newest of those supported
    return requested.stream()
        .filter(supported::contains)
        .map(version -> new AbstractMap.SimpleEntry<>(version, parseSemanticVersion(version)))
        .filter(entry -> entry.getValue().isPresent())
        .max(Comparator.comparing(e -> e.getValue().get()))
        .map(Map.Entry::getKey)
        .orElse("NO_MATCH");
  }

  private static Optional<Semver> parseSemanticVersion(String subprotocol) {
    try {
      return Optional.of(new Semver(subprotocol));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getEndpointInstance(Class<T> clazz) {
    return (T) eventsWebSocketManager.get();
  }

  @Override
  public void modifyHandshake(
      ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    // Authentication happens before the upgrade (EventsWebSocketAuthenticationFilter refuses anonymous
    // and denied users), and the user id is read from the session principal: nothing is stored in
    // the endpoint-wide config user properties.
  }
}
