// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketEndpoint {

  private static final int MAX_IDLE_TIMEOUT = 90000;

  private final EventDispatcher       eventDispatcher;
  private final ObjectMapper          objectMapper;
  private final Map<String, Channel>  userChannelMap;
  private final ParticipantRepository participantRepository;
  private final RoomRepository        roomRepository;
  private final ParticipantService    participantService;

  @Inject
  public EventsWebSocketEndpoint(EventDispatcher eventDispatcher, ObjectMapper objectMapper,
    ParticipantRepository participantRepository, RoomRepository roomRepository, ParticipantService participantService) {
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
    this.participantRepository = participantRepository;
    this.roomRepository = roomRepository;
    this.participantService = participantService;
    this.userChannelMap = new HashMap<>();
  }

  @OnOpen
  public void onOpen(Session session) throws IOException, EncodeException {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;

    session.setMaxIdleTimeout(MAX_IDLE_TIMEOUT);
    session.getBasicRemote().sendObject(objectMapper.writeValueAsString(SessionOutEvent.create(queueId)));

    if (eventDispatcher.getConnection().isEmpty()) {
      ChatsLogger.error("RabbitMQ connection is not up!");
      return;
    }
    Optional<Channel> optionalChannel = eventDispatcher.createChannel();
    if (optionalChannel.isEmpty()) {
      ChatsLogger.error("Could not create RabbitMQ channel for websocket");
      return;
    }
    final Channel channel = optionalChannel.get();
    try {
      userChannelMap.putIfAbsent(userId + "/" + session.getId(), channel);
      channel.queueDeclare(userQueue, true, false, false, null);
      channel.exchangeDeclare(userId.toString(), "direct");
      channel.queueBind(userQueue, userId.toString(), "");
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        try {
          session.getBasicRemote().sendObject(message);
        } catch (EncodeException | IOException e) {
          ChatsLogger.warn(
            String.format("Error sending RabbitMQ message to websocket for user/queue '%s'. Message: ''%s",
              userQueue, message), e);
        }
      };
      channel.basicConsume(userQueue, true, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      ChatsLogger.warn(
        String.format("Error interacting with RabbitMQ for user/queue '%s'", userQueue));
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.warn(
          String.format("Error closing RabbitMQ connection channel for user/queue '%s'", userQueue));
      }
    }
  }

  @OnMessage
  public void onMessage(Session session, String message) throws EncodeException, IOException {
    try {
      if (objectMapper.readValue(message, PingPongDtoEvent.class).getType().equals("ping")) {
        session.getBasicRemote().sendObject(objectMapper.writeValueAsString(PingPongDtoEvent.create("pong")));
      }
    } catch (JsonProcessingException e) {
      // intentionally left blank
    }
  }

  @OnClose
  public void onClose(Session session) {
    closeSessionChannel(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    closeSessionChannel(session);
  }

  private String getUserIdFromSession(Session session) {
    return Optional.ofNullable((String)
      ((HttpSession) session.getUserProperties()
        .get(HttpSession.class.getName()))
        .getAttribute("userId")).orElseThrow(() ->
      new InternalErrorException("Session user not found!"));
  }

  private void closeSessionChannel(Session session) {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userSessionId = userId + "/" + queueId;
    if (userChannelMap.containsKey(userSessionId)) {
      try {
        userChannelMap.get(userSessionId).close();
        userChannelMap.remove(userSessionId);
        participantRepository.getByQueueId(queueId.toString()).ifPresent(
          participant -> roomRepository.getById(participant.getMeeting().getRoomId()).ifPresent(
            room -> participantService.removeMeetingParticipant(participant.getMeeting(), room, userId, queueId))
        );
      } catch (IOException | TimeoutException ignored) {
        // intentionally left blank
      }
    }
  }

  private static class SessionOutEvent {

    private final UUID   queueId;
    private final String type = "websocketConnected";

    public SessionOutEvent(UUID queueId) {
      this.queueId = queueId;
    }

    public static SessionOutEvent create(UUID queueId) {
      return new SessionOutEvent(queueId);
    }

    public UUID getQueueId() {
      return queueId;
    }

    public String getType() {
      return type;
    }
  }

  private static class PingPongDtoEvent {

    private String type;

    private static PingPongDtoEvent create(String type) {
      return new PingPongDtoEvent().type(type);
    }

    public String getType() {
      return type;
    }

    public PingPongDtoEvent type(String type) {
      this.type = type;
      return this;
    }
  }
}

