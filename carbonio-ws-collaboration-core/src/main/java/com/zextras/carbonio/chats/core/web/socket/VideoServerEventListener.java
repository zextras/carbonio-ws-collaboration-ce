// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.data.event.MeetingAudioAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantTalking;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpOffered;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import io.vavr.MatchError;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@Singleton
@WebListener
public class VideoServerEventListener implements ServletContextListener {

  private static final String JANUS_EVENTS = "janus-events";
  private static final String LOCAL = "local";
  private static final int JSEP_TYPE = 8;
  private static final int PLUGIN_TYPE = 64;
  private static final String TALKING = "talking";
  private static final String STOPPED_TALKING = "stopped-talking";
  private static final List<String> TALKING_TYPE_EVENTS = List.of(TALKING, STOPPED_TALKING);

  private final Connection rabbitMqConnection;
  private final ObjectMapper objectMapper;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final ParticipantRepository participantRepository;

  private enum EventType {
    AUDIO,
    VIDEOIN,
    VIDEOOUT,
    SCREEN
  }

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
    try (Channel channel = rabbitMqConnection.createChannel()) {
      channel.queueDeclare(JANUS_EVENTS, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        VideoServerEvent videoServerEvent = objectMapper.readValue(message, VideoServerEvent.class);
        switch (videoServerEvent.getType()) {
          case JSEP_TYPE:
            Optional.ofNullable(videoServerEvent.getEventInfo().getOwner()).ifPresent(owner -> {
              if (LOCAL.equalsIgnoreCase(owner)) {
                videoServerSessionRepository.getByConnectionId(String.valueOf(videoServerEvent.getSessionId()))
                  .ifPresent(
                    videoServerSession -> participantRepository.getBySessionId(videoServerSession.getSessionId())
                      .ifPresent(participant -> {
                        try {
                          EventType eventType = Match(videoServerEvent.getHandleId().toString()).of(
                            Case($(videoServerSession.getAudioHandleId()), EventType.AUDIO),
                            Case($(videoServerSession.getVideoInHandleId()), EventType.VIDEOIN),
                            Case($(videoServerSession.getVideoOutHandleId()), EventType.VIDEOOUT),
                            Case($(videoServerSession.getScreenHandleId()), EventType.SCREEN)
                          );
                          RtcSessionDescription rtcSessionDescription = videoServerEvent.getEventInfo()
                            .getRtcSessionDescription();
                          switch (rtcSessionDescription.getType()) {
                            case OFFER:
                              send(channel, participant.getUserId(), objectMapper.writeValueAsString(
                                MeetingSdpOffered.create()
                                  .meetingId(UUID.fromString(participant.getMeeting().getId()))
                                  .userId(UUID.fromString(participant.getUserId()))
                                  .mediaType(eventType == EventType.SCREEN ? MediaType.SCREEN : MediaType.VIDEO)
                                  .sdp(rtcSessionDescription.getSdp())));
                              break;
                            case ANSWER:
                              switch (eventType) {
                                case AUDIO:
                                  send(channel, participant.getUserId(), objectMapper.writeValueAsString(
                                    MeetingAudioAnswered.create()
                                      .meetingId(UUID.fromString(participant.getMeeting().getId()))
                                      .userId(UUID.fromString(participant.getUserId()))
                                      .sdp(rtcSessionDescription.getSdp())));
                                  break;
                                case VIDEOIN:
                                case VIDEOOUT:
                                case SCREEN:
                                  send(channel, participant.getUserId(), objectMapper.writeValueAsString(
                                    MeetingSdpAnswered.create()
                                      .meetingId(UUID.fromString(participant.getMeeting().getId()))
                                      .userId(UUID.fromString(participant.getUserId()))
                                      .mediaType(eventType == EventType.SCREEN ? MediaType.SCREEN : MediaType.VIDEO)
                                      .sdp(rtcSessionDescription.getSdp())));
                              }
                              break;
                            default:
                              break;
                          }
                        } catch (MatchError m) {
                          ChatsLogger.error("Invalid event handle id: " + m.getObject());
                        } catch (JsonProcessingException e) {
                          ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                        }
                      }));
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
                        send(channel, participant.getUserId(), objectMapper.writeValueAsString(
                          MeetingParticipantTalking.create()
                            .meetingId(UUID.fromString(participant.getMeeting().getId()))
                            .userId(UUID.fromString(participant.getUserId()))
                            .isTalking(TALKING.equals(videoServerEvent.getEventInfo().getEventData().getAudioBridge()))
                        ));
                      } catch (JsonProcessingException e) {
                        ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                      }
                    });
                }
              });
            break;
          default:
            break;
        }
      };
      channel.basicConsume(JANUS_EVENTS, true, deliverCallback, consumerTag -> {
      });
    } catch (IOException | TimeoutException e) {
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

  }
}
