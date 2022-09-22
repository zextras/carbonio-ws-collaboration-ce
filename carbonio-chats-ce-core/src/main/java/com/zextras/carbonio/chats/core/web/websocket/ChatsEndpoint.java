package com.zextras.carbonio.chats.core.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/events")
public class ChatsEndpoint {

  private final Connection            connection;
  private final AuthenticationService authenticationService;
  private final ObjectMapper          objectMapper;
  private final Map<String, Channel>  sessionsMap = new HashMap<>();

  public ChatsEndpoint(
    Connection connection,
    AuthenticationService authenticationService,
    ObjectMapper objectMapper
  ) {
    this.connection = connection;
    this.authenticationService = authenticationService;
    this.objectMapper = objectMapper;
  }

  @OnOpen
  public void onOpen(Session session) throws IOException, EncodeException {
    session.getBasicRemote().sendObject("connected");
    session.setMaxIdleTimeout(-1);
  }

  @OnMessage
  public void onMessage(Session session, String message) throws IOException, TimeoutException, EncodeException {
    if (message.contains("=")) {
      Optional<AuthenticationMethod> methodOpt = Arrays.stream(AuthenticationMethod.values())
        .filter(method -> method.name().equals(message.substring(0, message.indexOf("=")))).findAny();
      if (methodOpt.isPresent()) {
        if (sessionsMap.containsKey(session.getId())) {
          sessionsMap.get(session.getId()).close();
        }
        Optional<String> userIdOpt = authenticationService.validateCredentials(
            Map.of(methodOpt.get(), message.substring(message.indexOf("=") + 1)));
        if (userIdOpt.isPresent()) {
          session.getBasicRemote().sendObject(objectMapper.writeValueAsString(SessionOut.create(session.getId())));
          sessionsMap.put(session.getId(), startBridge(session, userIdOpt.get()));
        } else {
          session.getBasicRemote().sendObject("Unauthorized");
          throw new UnauthorizedException();
        }
      }
    }
  }

  private Channel startBridge(Session session, String userId) {
    try {
      Channel channel = connection.createChannel();
      channel.queueDeclare(userId, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        try {
          session.getBasicRemote().sendObject(message);
        } catch (EncodeException e) {
          ChatsLogger.error(
            String.format("Error sending RabbitMQ message to websocket for user '%s'. Message: ''%s",
              userId, message), e);
        }
      };
      channel.basicConsume(userId, true, deliverCallback, consumerTag -> {
      });
      return channel;
    } catch (IOException e) {
      throw new InternalErrorException(String.format("Error running RabbitMQ client for user '%s'", userId), e);
    }
  }

  @OnClose
  public void onClose(Session session) throws IOException, EncodeException, TimeoutException {
    if (sessionsMap.containsKey(session.getId())) {
      sessionsMap.get(session.getId()).close();
      sessionsMap.remove(session.getId());
    }
    session.getBasicRemote().sendObject("Disconnected");
  }

  @OnError
  public void onError(Session session, Throwable throwable) throws IOException, TimeoutException {
    if (sessionsMap.containsKey(session.getId())) {
      sessionsMap.get(session.getId()).close();
      sessionsMap.remove(session.getId());
    }
    ChatsLogger.error(ChatsEndpoint.class, "Error on Chats websocket", throwable);
  }

  private static class SessionOut {

    private final String sessionId;

    public SessionOut(String sessionId) {
      this.sessionId = sessionId;
    }

    public static SessionOut create(String sessionId) {
      return new SessionOut(sessionId);
    }

    public String getSessionId() {
      return sessionId;
    }
  }
}

