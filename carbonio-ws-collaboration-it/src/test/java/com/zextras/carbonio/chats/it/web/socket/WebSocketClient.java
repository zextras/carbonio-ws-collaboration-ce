// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.provider.impl.ObjectMapperProvider;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebSocketClient extends Endpoint {

  private ObjectMapper mapper = ObjectMapperProvider.getObjectMapper();
  private final List<String> receivedMessages = new CopyOnWriteArrayList<>();
  private Session session;
  private volatile boolean connected = false;

  @Override
  public void onOpen(Session session, EndpointConfig config) {
    ChatsLogger.info(String.format("WebSocket connection opened: %s", session.getId()));
    this.session = session;
    this.connected = true;

    session.addMessageHandler(String.class, this::handleMessage);
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    ChatsLogger.info(String.format("WebSocket closed: %s", closeReason));
    this.session = null;
    this.connected = false;
  }

  @Override
  public void onError(Session session, Throwable thr) {
    ChatsLogger.warn("Websocket error occurred", thr);
    this.connected = false;
  }

  private void handleMessage(String message) {
    ChatsLogger.info(String.format("WebSocket message received: %s", message));
    receivedMessages.add(message);
  }

  public List<String> getMessages() {
    return new ArrayList<>(receivedMessages);
  }

  public void sendMessage(Object object) throws IOException, EncodeException {
    if (session != null && connected) {
      String message = mapper.writeValueAsString(object);
      ChatsLogger.info(String.format("WebSocket send message: %s", message));
      session.getBasicRemote().sendObject(message);
    } else {
      ChatsLogger.warn("Cannot send message - session not connected");
    }
  }

  public boolean isConnected() {
    return connected && session != null && session.isOpen();
  }

  public void disconnect() throws IOException {
    if (session != null && session.isOpen()) {
      ChatsLogger.info("Disconnecting WebSocket");
      session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Test completed"));
    }
  }
}
