package com.zextras.carbonio.chats.core.web.socket;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class EventsWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

  private final EventsWebSocketEndpoint eventsWebSocketEndpoint;

  public EventsWebSocketEndpointConfigurator(
    EventsWebSocketEndpoint eventsWebSocketEndpoint
  ) {
    this.eventsWebSocketEndpoint = eventsWebSocketEndpoint;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getEndpointInstance(Class<T> clazz) {
    return (T) eventsWebSocketEndpoint;
  }

  @Override
  public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    config.getUserProperties().put(HttpSession.class.getName(), httpSession);
  }

}