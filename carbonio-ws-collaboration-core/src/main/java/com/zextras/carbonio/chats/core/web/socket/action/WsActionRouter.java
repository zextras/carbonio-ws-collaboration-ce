// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.WsError;
import com.zextras.carbonio.async.model.WsPong;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.socket.action.impl.TypingHandler;
import jakarta.websocket.Session;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

@Singleton
public class WsActionRouter {

  private final Map<WsAction, WsActionHandler> handlers;
  private final ObjectMapper objectMapper;

  @Inject
  public WsActionRouter(TypingHandler typingHandler, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.handlers = new EnumMap<>(WsAction.class);
    handlers.put(WsAction.TYPING, typingHandler);
  }

  public void route(String userId, String message, Session session) {
    try {
      JsonNode node = objectMapper.readTree(message);
      JsonNode actionNode = node.get("action");
      if (actionNode == null || actionNode.isNull()) {
        sendError(session, null, "Missing 'action' field");
        return;
      }

      String actionValue = actionNode.asText();
      WsAction action;
      try {
        action = WsAction.fromValue(actionValue);
      } catch (IllegalArgumentException e) {
        sendError(session, getRequestId(node), "Unknown action: " + actionValue);
        return;
      }

      // Handle ping directly (no need for a full handler)
      if (action == WsAction.PING) {
        sendPong(session);
        return;
      }

      WsActionHandler handler = handlers.get(action);
      if (handler == null) {
        sendError(session, getRequestId(node), "No handler for action: " + actionValue);
        return;
      }

      handler.handle(userId, node, session);
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Error routing WebSocket action for user '%s': %s", userId, e.getMessage()));
      sendError(session, null, "Internal error processing action");
    }
  }

  private String getRequestId(JsonNode node) {
    JsonNode requestIdNode = node.get("requestId");
    return (requestIdNode != null && !requestIdNode.isNull()) ? requestIdNode.asText() : null;
  }

  private void sendPong(Session session) {
    if (session == null || !session.isOpen()) return;
    try {
      WsPong pong = WsPong.create();
      pong.setSentDate(OffsetDateTime.now());
      session.getAsyncRemote().sendObject(objectMapper.writeValueAsString(pong));
    } catch (Exception e) {
      ChatsLogger.warn("Error sending pong via WebSocket: " + e.getMessage());
    }
  }

  private void sendError(Session session, String requestId, String errorMessage) {
    if (session == null || !session.isOpen()) return;
    try {
      WsError error = WsError.create();
      error.setSentDate(OffsetDateTime.now());
      error.setMessage(errorMessage);
      if (requestId != null) {
        error.setRequestId(requestId);
      }
      session.getAsyncRemote().sendObject(objectMapper.writeValueAsString(error));
    } catch (Exception e) {
      ChatsLogger.warn("Error sending error response via WebSocket: " + e.getMessage());
    }
  }
}
