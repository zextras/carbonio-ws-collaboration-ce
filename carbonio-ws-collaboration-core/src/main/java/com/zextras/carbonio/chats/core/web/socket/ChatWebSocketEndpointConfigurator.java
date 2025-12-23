// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Configurator for the Chat WebSocket endpoint. Handles endpoint instantiation and HTTP session
 * propagation to WebSocket sessions.
 */
public class ChatWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

  private final ChatWebSocketManager chatWebSocketManager;

  public ChatWebSocketEndpointConfigurator(ChatWebSocketManager chatWebSocketManager) {
    this.chatWebSocketManager = chatWebSocketManager;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getEndpointInstance(Class<T> clazz) {
    return (T) chatWebSocketManager;
  }

  @Override
  public void modifyHandshake(
      ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    config.getUserProperties().put(HttpSession.class.getName(), httpSession);
  }
}
