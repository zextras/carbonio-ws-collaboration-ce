// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.WebsocketConnected;
import com.zextras.carbonio.ws.core.events.RabbitSessions;
import com.zextras.carbonio.ws.core.security.SecurityAttributes;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/events")
public class EventsSocket {

  @Inject SecurityIdentity identity;
  @Inject RabbitSessions sessions;
  @Inject ObjectMapper mapper;

  private final Map<String, String> queueByConnection = new ConcurrentHashMap<>();

  /** Non-reactive return type: runs on a worker thread, so the blocking queue setup is fine. */
  @OnOpen
  public String onOpen(WebSocketConnection connection) throws JsonProcessingException {
    UserPrincipal user = identity.getAttribute(SecurityAttributes.USER);
    String queueId = UUID.randomUUID().toString();
    queueByConnection.put(connection.id(), queueId);
    sessions.setup(user.id().toString(), queueId, connection::sendTextAndAwait);
    return mapper.writeValueAsString(
        WebsocketConnected.create()
            .queueId(UUID.fromString(queueId))
            .type(EventType.WEBSOCKET_CONNECTED)
            .sentDate(OffsetDateTime.now(ZoneOffset.UTC)));
  }

  @OnClose
  public void onClose(WebSocketConnection connection) {
    String queueId = queueByConnection.remove(connection.id());
    if (queueId != null) {
      sessions.teardown(queueId);
    }
  }
}
