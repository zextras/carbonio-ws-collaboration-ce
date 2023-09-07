// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.event.MeetingAudioAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingMediaStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantTalking;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpOffered;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import io.vavr.MatchError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@Singleton
@WebListener
public class VideoServerEventListener implements ServletContextListener {

  private static final String       JANUS_EVENTS         = "janus-events";
  private static final String       LOCAL                = "local";
  private static final int          JSEP_TYPE            = 8;
  private static final int          PLUGIN_TYPE          = 64;
  private static final String       TALKING              = "talking";
  private static final String       STOPPED_TALKING      = "stopped-talking";
  private static final List<String> TALKING_TYPE_EVENTS  = List.of(TALKING, STOPPED_TALKING);
  private static final String       SUBSCRIBE_TYPE_EVENT = "subscribing";
  private static final String       PUBLISHED            = "published";
  private static final String       UNPUBLISHED          = "unpublished";
  private static final List<String> PUBLISH_TYPE_EVENTS  = List.of(PUBLISHED, UNPUBLISHED);

  private final Connection                   rabbitMqConnection;
  private final ObjectMapper                 objectMapper;
  private final VideoServerSessionRepository videoServerSessionRepository;

  private enum EventType {
    AUDIO,
    VIDEOIN,
    VIDEOOUT,
    SCREEN
  }

  @Inject
  public VideoServerEventListener(Optional<Connection> rabbitMqConnection, ObjectMapper objectMapper,
    VideoServerSessionRepository videoServerSessionRepository) {
    this.rabbitMqConnection = rabbitMqConnection.orElse(null);
    this.objectMapper = objectMapper;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    if (Optional.ofNullable(rabbitMqConnection).isEmpty()) {
      throw new InternalErrorException("RabbitMQ connection is not up!");
    }
    Channel channel;
    try {
      channel = rabbitMqConnection.createChannel();
    } catch (IOException e) {
      ChatsLogger.error("Error creating RabbitMQ connection channel for videoserver events ", e);
      return;
    }
    try {
      channel.queueDeclare(JANUS_EVENTS, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        VideoServerEvent videoServerEvent = objectMapper.readValue(message, VideoServerEvent.class);
        ChatsLogger.info("Videoserver event: " + objectMapper.writeValueAsString(videoServerEvent));
        AtomicReference<String> audioHandleId = new AtomicReference<>();
        AtomicReference<String> videoInHandleId = new AtomicReference<>();
        AtomicReference<String> videoOutHandleId = new AtomicReference<>();
        AtomicReference<String> screenHandleId = new AtomicReference<>();
        switch (videoServerEvent.getType()) {
          case JSEP_TYPE:
            Optional.ofNullable(videoServerEvent.getEventInfo().getOwner()).ifPresent(owner -> {
              if (LOCAL.equalsIgnoreCase(owner)) {
                videoServerSessionRepository.getByConnectionId(String.valueOf(videoServerEvent.getSessionId()))
                  .ifPresent(videoServerSession -> {
                    audioHandleId.set(videoServerSession.getAudioHandleId());
                    videoInHandleId.set(videoServerSession.getVideoInHandleId());
                    videoOutHandleId.set(videoServerSession.getVideoOutHandleId());
                    screenHandleId.set(videoServerSession.getScreenHandleId());
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
                          send(channel, videoServerSession.getUserId(), objectMapper.writeValueAsString(
                            MeetingSdpOffered.create()
                              .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                              .userId(UUID.fromString(videoServerSession.getUserId()))
                              .mediaType(eventType == EventType.SCREEN ? MediaType.SCREEN : MediaType.VIDEO)
                              .sdp(rtcSessionDescription.getSdp())));
                          ChatsLogger.info("Sent offer " + objectMapper.writeValueAsString(videoServerEvent));
                          break;
                        case ANSWER:
                          switch (eventType) {
                            case AUDIO:
                              send(channel, videoServerSession.getUserId(), objectMapper.writeValueAsString(
                                MeetingAudioAnswered.create()
                                  .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                                  .userId(UUID.fromString(videoServerSession.getUserId()))
                                  .sdp(rtcSessionDescription.getSdp())));
                              ChatsLogger.info("Sent audio " + objectMapper.writeValueAsString(videoServerEvent));
                              break;
                            case VIDEOIN:
                            case VIDEOOUT:
                            case SCREEN:
                              send(channel, videoServerSession.getUserId(), objectMapper.writeValueAsString(
                                MeetingSdpAnswered.create()
                                  .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                                  .userId(UUID.fromString(videoServerSession.getUserId()))
                                  .mediaType(eventType == EventType.SCREEN ? MediaType.SCREEN : MediaType.VIDEO)
                                  .sdp(rtcSessionDescription.getSdp())));
                              ChatsLogger.info("Sent answer " + objectMapper.writeValueAsString(videoServerEvent));
                          }
                          break;
                        default:
                          ChatsLogger.info(
                            "Skip not valid handle " + objectMapper.writeValueAsString(videoServerEvent));
                          break;
                      }
                    } catch (MatchError m) {
                      ChatsLogger.warn(
                        "Invalid event handle id: " + m.getObject()
                          + "\naudio handle id : " + audioHandleId.get()
                          + "\nvideo in handle id : " + videoInHandleId.get()
                          + "\nvideo out handle id : " + videoOutHandleId.get()
                          + "\nscreen handle id : " + screenHandleId.get());
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
                  videoServerSessionRepository.getByConnectionId(String.valueOf(videoServerEvent.getSessionId()))
                    .ifPresent(videoServerSession -> {
                      String meetingId = videoServerSession.getId().getMeetingId();
                      List<VideoServerSession> sessionList = videoServerSessionRepository.getByMeetingId(meetingId);
                      sessionList.forEach(session -> {
                        try {
                          send(channel, session.getUserId(), objectMapper.writeValueAsString(
                            MeetingParticipantTalking.create()
                              .meetingId(UUID.fromString(meetingId))
                              .userId(UUID.fromString(videoServerSession.getUserId()))
                              .isTalking(
                                TALKING.equals(videoServerEvent.getEventInfo().getEventData().getAudioBridge()))
                          ));
                        } catch (JsonProcessingException e) {
                          ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                        }
                      });
                    });
                }
              });
            Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getEvent())
              .ifPresent(eventType -> {
                if (SUBSCRIBE_TYPE_EVENT.equals(eventType)) {
                  videoServerSessionRepository.getByConnectionId(String.valueOf(videoServerEvent.getSessionId()))
                    .ifPresent(videoServerSession -> {
                      try {
                        send(channel, videoServerSession.getUserId(),
                          objectMapper.writeValueAsString(videoServerEvent));
                      } catch (JsonProcessingException e) {
                        ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                      }
                    });
                }
              });
            Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getEvent())
              .ifPresent(eventType -> {
                if (PUBLISH_TYPE_EVENTS.contains(eventType)) {
                  String feedId = videoServerEvent.getEventInfo().getEventData().getId();
                  videoServerSessionRepository.getByUserId(feedId.split("/")[0])
                    .ifPresent(videoServerSession -> {
                      String meetingId = videoServerSession.getId().getMeetingId();
                      List<VideoServerSession> sessionList = videoServerSessionRepository.getByMeetingId(meetingId);
                      sessionList.forEach(session -> {
                        try {
                          send(channel, session.getUserId(), objectMapper.writeValueAsString(
                            MeetingMediaStreamChanged.create()
                              .meetingId(UUID.fromString(meetingId))
                              .userId(UUID.fromString(videoServerSession.getUserId()))
                              .mediaType(MediaType.valueOf(feedId.split("/")[1].toUpperCase()))
                              .active(PUBLISHED.equals(videoServerEvent.getEventInfo().getEventData().getEvent()))
                          ));
                        } catch (JsonProcessingException e) {
                          ChatsLogger.debug("Error during serialization of " + videoServerEvent);
                        }
                      });
                    });
                }
              });
            break;
          default:
            ChatsLogger.info("Skip " + objectMapper.writeValueAsString(message));
            break;
        }
      };
      channel.basicConsume(JANUS_EVENTS, true, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.error("Error closing RabbitMQ connection channel for videoserver events");
      }
      ChatsLogger.error("Closed RabbitMQ connection channel for videoserver events");
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
