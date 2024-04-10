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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.EncodeException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketEndpoint {

  private static final int MAX_IDLE_TIMEOUT = 90000;

  private final EventDispatcher eventDispatcher;
  private final ObjectMapper objectMapper;
  private final ParticipantRepository participantRepository;
  private final RoomRepository roomRepository;
  private final ParticipantService participantService;

  @Inject
  public EventsWebSocketEndpoint(
      EventDispatcher eventDispatcher,
      ObjectMapper objectMapper,
      ParticipantRepository participantRepository,
      RoomRepository roomRepository,
      ParticipantService participantService) {
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
    this.participantRepository = participantRepository;
    this.roomRepository = roomRepository;
    this.participantService = participantService;
  }

  @OnOpen
  public void onOpen(Session session) throws IOException, EncodeException {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;

    session.setMaxIdleTimeout(MAX_IDLE_TIMEOUT);
    session
        .getBasicRemote()
        .sendObject(objectMapper.writeValueAsString(SessionOutEvent.create(queueId)));

    Optional<Channel> optionalChannel = eventDispatcher.getChannel();
    if (optionalChannel.isEmpty()) {
      ChatsLogger.error("RabbitMQ connection channel is not up!");
      return;
    }
    final Channel channel = optionalChannel.get();
    try {
      channel.queueDeclare(userQueue, false, false, true, null);
      channel.exchangeDeclare(userId.toString(), "direct");
      channel.queueBind(userQueue, userId.toString(), "");
      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
              if (session.isOpen()) {
                session.getBasicRemote().sendObject(message);
              }
            } catch (EncodeException | IOException e) {
              ChatsLogger.warn(
                  String.format(
                      "Error sending RabbitMQ message to websocket for user/queue '%s'. Message:"
                          + " ''%s",
                      userQueue, message),
                  e);
            }
          };
      channel.basicConsume(userQueue, true, deliverCallback, consumerTag -> {});
    } catch (IOException e) {
      ChatsLogger.warn(
          String.format("Error interacting with RabbitMQ for user/queue '%s'", userQueue));
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.warn(
            String.format(
                "Error closing RabbitMQ connection channel for user/queue '%s'", userQueue));
      }
    }
  }

  @OnMessage
  public void onMessage(Session session, String message) throws EncodeException, IOException {
    try {
      if (objectMapper.readValue(message, PingPongDtoEvent.class).getType().equals("ping")) {
        session
            .getBasicRemote()
            .sendObject(objectMapper.writeValueAsString(PingPongDtoEvent.create("pong")));
      }
    } catch (JsonProcessingException e) {
      // intentionally left blank
    }
  }

  @OnClose
  public void onClose(Session session) {
    closeSession(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    closeSession(session);
  }

  private String getUserIdFromSession(Session session) {
    return Optional.ofNullable(
            (String)
                ((HttpSession) session.getUserProperties().get(HttpSession.class.getName()))
                    .getAttribute("userId"))
        .orElseThrow(() -> new InternalErrorException("Session user not found!"));
  }

  private void closeSession(Session session) {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;
    Optional<Channel> optionalChannel = eventDispatcher.getChannel();
    if (optionalChannel.isEmpty()) {
      ChatsLogger.error("RabbitMQ connection channel is not up!");
      return;
    }
    final Channel channel = optionalChannel.get();
    try {
      channel.queueDelete(userQueue);
    } catch (IOException e) {
      ChatsLogger.warn(
          String.format("Error interacting with RabbitMQ for user/queue '%s'", userQueue));
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.warn(
            String.format(
                "Error closing RabbitMQ connection channel for user/queue '%s'", userQueue));
      }
    }
    participantRepository
        .getByQueueId(queueId.toString())
        .ifPresent(
            participant ->
                roomRepository
                    .getById(participant.getMeeting().getRoomId())
                    .ifPresent(
                        room ->
                            participantService.removeMeetingParticipant(
                                participant.getMeeting(), room, userId, queueId)));
  }

  private static class SessionOutEvent {

    private final UUID queueId;
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
