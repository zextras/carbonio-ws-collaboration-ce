// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@Singleton
@WebListener
public class VideoServerEventListener implements ServletContextListener {

  private static final String       JANUS_EVENTS        = "janus-events";
  private static final String       LOCAL               = "local";
  private static final int          JSEP_TYPE           = 8;
  private static final int          PLUGIN_TYPE         = 64;
  private static final List<String> TALKING_TYPE_EVENTS = List.of("talking", "stopped-talking");

  private final Connection                   rabbitMqConnection;
  private final ObjectMapper                 objectMapper;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final ParticipantRepository        participantRepository;

  @Inject
  public VideoServerEventListener(Optional<Connection> rabbitMqConnection, ObjectMapper objectMapper,
    VideoServerSessionRepository videoServerSessionRepository, ParticipantRepository participantRepository) {
    this.rabbitMqConnection = rabbitMqConnection.orElse(null);
    this.objectMapper = objectMapper;
    this.videoServerSessionRepository = videoServerSessionRepository;
    this.participantRepository = participantRepository;
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    if (Optional.ofNullable(rabbitMqConnection).isEmpty()) {
      throw new InternalErrorException("RabbitMQ connection is not up!");
    }
    final Channel channel;
    try {
      channel = rabbitMqConnection.createChannel();
    } catch (IOException e) {
      throw new InternalErrorException("Error creating RabbitMQ connection channel for videoserver events", e);
    }
    try {
      channel.queueDeclare(JANUS_EVENTS, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        //TODO remove log
        ChatsLogger.debug("Videoserver event: " + message);
        VideoServerEvent videoServerEvent = objectMapper.readValue(message, VideoServerEvent.class);
        switch (videoServerEvent.getType()) {
          case JSEP_TYPE:
            Optional.ofNullable(videoServerEvent.getEventInfo().getOwner()).ifPresent(owner -> {
              if (LOCAL.equalsIgnoreCase(owner)) {
                videoServerSessionRepository.getByConnectionId(String.valueOf(videoServerEvent.getSessionId()))
                  .flatMap(
                    videoServerSession -> participantRepository.getBySessionId(videoServerSession.getSessionId()))
                  .ifPresent(participant -> {
                    try {
                      send(channel, participant.getUserId(), objectMapper.writeValueAsString(
                        videoServerEvent.meetingId(participant.getMeeting().getId())));
                    } catch (JsonProcessingException e) {
                      ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                    }
                  });
              }
            });
            break;
          case PLUGIN_TYPE:
            Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getAudioBridge())
              .ifPresent(eventType -> {
                if (TALKING_TYPE_EVENTS.contains(eventType)) {
                  Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getId())
                    .flatMap(participantRepository::getBySessionId).ifPresent(participant -> {
                      try {
                        send(channel, participant.getUserId(),
                          objectMapper.writeValueAsString(
                            videoServerEvent.meetingId(participant.getMeeting().getId())));
                      } catch (JsonProcessingException e) {
                        ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                      }
                    });
                }
              });
            break;
          default:
            ChatsLogger.debug("Skip videoserver event: " + message);
            break;
        }
      };
      channel.basicConsume(JANUS_EVENTS, true, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.warn("Error closing RabbitMQ connection channel for videoserver events");
      }
      throw new InternalErrorException("Error with RabbitMQ connection channel for videoserver events", e);
    }
  }

  private void send(Channel channel, String userId, String message) {
    try {
      if (Optional.ofNullable(rabbitMqConnection).isEmpty()) {
        return;
      }
      channel.exchangeDeclare(userId, "direct");
      channel.basicPublish(userId, "", null, message.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        //nothing much we can do
      }
      ChatsLogger.warn(String.format("Unable to send message to %s", userId), e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    try {
      rabbitMqConnection.close();
    } catch (IOException e) {
      ChatsLogger.warn("Unable to close RabbitMQ connection due to " + e);
    }
  }
}
