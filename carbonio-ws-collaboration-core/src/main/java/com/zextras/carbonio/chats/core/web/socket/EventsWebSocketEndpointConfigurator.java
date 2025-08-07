// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.vdurmont.semver4j.Semver;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EventsWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

  private final EventsWebSocketManager eventsWebSocketManager;

  public EventsWebSocketEndpointConfigurator(EventsWebSocketManager eventsWebSocketManager) {
    this.eventsWebSocketManager = eventsWebSocketManager;
  }

  @Override
  public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
    // OLD_CLIENT_FALLBACK
    // if the version is not present, it's set to empty string to execute event type rename
    // migration for mobile app compatibility
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
        .orElse("");
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
    return (T) eventsWebSocketManager;
  }

  @Override
  public void modifyHandshake(
      ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    config.getUserProperties().put(HttpSession.class.getName(), httpSession);
  }
}
