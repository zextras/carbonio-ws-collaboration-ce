package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class EventsWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

  private final Connection   connection;
  private final ObjectMapper objectMapper;

  public EventsWebSocketEndpointConfigurator(
    Connection connection,
    ObjectMapper objectMapper
  ) {
    this.connection = connection;
    this.objectMapper = objectMapper;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getEndpointInstance(Class<T> clazz) {
    return (T) new EventsWebSocketEndpoint(connection, objectMapper);
  }

  @Override
  public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    config.getUserProperties().put(HttpSession.class.getName(), httpSession);
  }

}