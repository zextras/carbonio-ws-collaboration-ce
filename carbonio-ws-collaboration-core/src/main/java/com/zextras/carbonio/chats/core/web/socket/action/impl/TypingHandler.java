// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.Typing;
import com.zextras.carbonio.async.model.WsError;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.web.socket.action.WsActionHandler;
import jakarta.websocket.Session;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Typing uses a stateless relay model: the backend forwards typing:started and typing:stopped
 * events idempotently without maintaining any server-side state. Unlike presence (which guarantees
 * transition delivery and handles crash recovery), typing events are best-effort. Clients MUST
 * implement a local timeout (7000ms recommended) to hide the typing indicator if no new
 * typing:started event is received — the typing:stopped event improves responsiveness but cannot be
 * relied upon as the sole stop signal, since the sending instance may crash or the WebSocket may
 * drop before it is sent. This trade-off (no server-side state, no DB, no cross-instance
 * coordination) is deliberate: typing events are frequent but ephemeral, and the reduced backend
 * complexity outweighs the occasional 7s timeout-based stop.
 */
@Singleton
public class TypingHandler implements WsActionHandler {

  private final EventDispatcher eventDispatcher;
  private final SubscriptionRepository subscriptionRepository;
  private final ObjectMapper objectMapper;

  @Inject
  public TypingHandler(
      EventDispatcher eventDispatcher,
      SubscriptionRepository subscriptionRepository,
      ObjectMapper objectMapper) {
    this.eventDispatcher = eventDispatcher;
    this.subscriptionRepository = subscriptionRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(String userId, JsonNode payload, Session session) {
    try {
      String roomId = getRequiredText(payload, "roomId");

      List<String> memberIds = subscriptionRepository.getUserIdsByRoomId(roomId);
      if (!memberIds.contains(userId)) {
        sendError(session, getOptionalText(payload, "requestId"), "ERR_FORBIDDEN", "Not a member");
        return;
      }

      String status = getOptionalText(payload, "status");
      // Normalise: anything other than "stopped" is treated as "started"
      Typing.StatusEnum normalizedStatus =
          "stopped".equalsIgnoreCase(status)
              ? Typing.StatusEnum.STOPPED
              : Typing.StatusEnum.STARTED;

      String json =
          objectMapper.writeValueAsString(
              Typing.create()
                  .roomId(roomId)
                  .userId(userId)
                  .status(normalizedStatus)
                  .timestamp(OffsetDateTime.now())
                  .sentDate(OffsetDateTime.now()));

      // Relay to all room members except the sender — stateless, best-effort
      for (String memberId : memberIds) {
        if (!memberId.equals(userId)) {
          try {
            eventDispatcher.sendRawToUserExchange(memberId, json);
          } catch (Exception e) {
            ChatsLogger.warn(
                String.format(
                    "TypingHandler: failed to relay typing:%s to user '%s': %s",
                    normalizedStatus, memberId, e.getMessage()));
          }
        }
      }
    } catch (IllegalArgumentException e) {
      ChatsLogger.warn(
          String.format("Bad request in TypingHandler for user '%s': %s", userId, e.getMessage()));
      sendError(
          session, getOptionalText(payload, "requestId"), "ERR_BAD_REQUEST", "Invalid request");
    } catch (Exception e) {
      ChatsLogger.error(
          String.format("Unexpected error in TypingHandler for user '%s'", userId), e);
      sendError(
          session, getOptionalText(payload, "requestId"), "ERR_INTERNAL", "Internal server error");
    }
  }

  private void sendError(Session session, String requestId, String errorCode, String errorMessage) {
    if (session == null || !session.isOpen()) return;
    try {
      WsError error = WsError.create();
      error.setSentDate(OffsetDateTime.now());
      error.setCode(errorCode);
      error.setMessage(errorMessage);
      if (requestId != null) {
        error.setRequestId(requestId);
      }
      session.getAsyncRemote().sendObject(objectMapper.writeValueAsString(error));
    } catch (Exception e) {
      ChatsLogger.warn("Error sending error to session: " + e.getMessage());
    }
  }

  private String getRequiredText(JsonNode payload, String field) {
    JsonNode node = payload.get(field);
    if (node == null || node.isNull() || node.asText().isBlank()) {
      throw new IllegalArgumentException("Missing required field: " + field);
    }
    return node.asText();
  }

  private String getOptionalText(JsonNode payload, String field) {
    JsonNode node = payload.get(field);
    return (node != null && !node.isNull()) ? node.asText() : null;
  }
}
