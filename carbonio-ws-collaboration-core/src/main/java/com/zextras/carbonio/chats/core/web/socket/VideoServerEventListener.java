// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.event.MeetingAudioAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingMediaStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantSubscribed;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantTalking;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpOffered;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.StreamData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@Singleton
@WebListener
public class VideoServerEventListener implements ServletContextListener {

  private static final String       JANUS_EVENTS           = "janus-events";
  private static final String       LOCAL                  = "local";
  private static final int          JSEP_TYPE              = 8;
  private static final int          PLUGIN_TYPE            = 64;
  private static final String       TALKING                = "talking";
  private static final String       STOPPED_TALKING        = "stopped-talking";
  private static final List<String> TALKING_TYPE_EVENTS    = List.of(TALKING, STOPPED_TALKING);
  private static final String       SUBSCRIBING_TYPE_EVENT = "subscribing";
  private static final String       UPDATED_TYPE_EVENT     = "updated";
  private static final String       PUBLISHED              = "published";

  private final EventDispatcher              eventDispatcher;
  private final ObjectMapper                 objectMapper;
  private final VideoServerSessionRepository videoServerSessionRepository;

  private enum EventType {
    AUDIO,
    VIDEOIN,
    VIDEOOUT,
    SCREEN
  }

  @Inject
  public VideoServerEventListener(EventDispatcher eventDispatcher, ObjectMapper objectMapper,
    VideoServerSessionRepository videoServerSessionRepository) {
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    if (eventDispatcher.getConnection().isEmpty()) {
      throw new InternalErrorException("RabbitMQ connection is not up!");
    }
    Optional<Channel> optionalChannel = eventDispatcher.createChannel();
    if (optionalChannel.isEmpty()) {
      ChatsLogger.error("Could not create RabbitMQ channel for websocket");
      return;
    }
    final Channel channel = optionalChannel.get();
    try {
      channel.queueDeclare(JANUS_EVENTS, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        ChatsLogger.error("Message: " + message);
        List<VideoServerEvent> videoServerEvents = objectMapper.readValue(message, objectMapper.getTypeFactory()
          .constructCollectionType(List.class, VideoServerEvent.class));
        videoServerEvents.forEach(videoServerEvent -> {
          switch (videoServerEvent.getType()) {
            case JSEP_TYPE:
              Optional.ofNullable(videoServerEvent.getEventInfo().getOwner()).ifPresent(owner -> {
                if (LOCAL.equalsIgnoreCase(owner)) {
                  videoServerSessionRepository.getByConnectionId(String.valueOf(videoServerEvent.getSessionId()))
                    .ifPresent(videoServerSession -> {
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
                            eventDispatcher.sendToUserExchange(videoServerSession.getUserId(),
                              MeetingSdpOffered.create()
                                .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                                .userId(UUID.fromString(videoServerSession.getUserId()))
                                .mediaType(eventType == EventType.SCREEN ? MediaType.SCREEN : MediaType.VIDEO)
                                .sdp(rtcSessionDescription.getSdp()));
                            break;
                          case ANSWER:
                            switch (eventType) {
                              case AUDIO:
                                eventDispatcher.sendToUserExchange(videoServerSession.getUserId(),
                                  MeetingAudioAnswered.create()
                                    .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                                    .userId(UUID.fromString(videoServerSession.getUserId()))
                                    .sdp(rtcSessionDescription.getSdp()));
                                break;
                              case VIDEOIN:
                              case VIDEOOUT:
                              case SCREEN:
                                eventDispatcher.sendToUserExchange(videoServerSession.getUserId(),
                                  MeetingSdpAnswered.create()
                                    .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                                    .userId(UUID.fromString(videoServerSession.getUserId()))
                                    .mediaType(eventType == EventType.SCREEN ? MediaType.SCREEN : MediaType.VIDEO)
                                    .sdp(rtcSessionDescription.getSdp()));
                            }
                            break;
                          default:
                            break;
                        }
                      } catch (MatchError m) {
                        ChatsLogger.error("Invalid event handle id: " + m.getObject());
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
                        List<String> sessionUserIds = videoServerSessionRepository.getByMeetingId(meetingId).stream()
                          .map(VideoServerSession::getUserId).collect(Collectors.toList());
                        eventDispatcher.sendToUserExchange(sessionUserIds,
                          MeetingParticipantTalking.create()
                            .meetingId(UUID.fromString(meetingId))
                            .userId(UUID.fromString(videoServerSession.getUserId()))
                            .isTalking(
                              TALKING.equals(videoServerEvent.getEventInfo().getEventData().getAudioBridge())));
                      });
                  }
                });
              Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getEvent())
                .ifPresent(eventType -> {
                  switch (eventType) {
                    case UPDATED_TYPE_EVENT:
                    case SUBSCRIBING_TYPE_EVENT:
                      Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getStreamList())
                        .ifPresent(streamData -> {
                          List<StreamData> streamDataList = streamData.stream()
                            .filter(stream -> Optional.ofNullable(stream.getFeedId()).isPresent())
                            .collect(Collectors.toList());
                          if (!streamDataList.isEmpty()) {
                            videoServerSessionRepository.getByConnectionId(
                              String.valueOf(videoServerEvent.getSessionId())).ifPresent(
                              videoServerSession -> eventDispatcher.sendToUserExchange(videoServerSession.getUserId(),
                                MeetingParticipantSubscribed.create()
                                  .meetingId(UUID.fromString(videoServerSession.getId().getMeetingId()))
                                  .userId(UUID.fromString(videoServerSession.getUserId()))
                                  .streams(streamDataList.stream().map(stream -> Feed.create()
                                      .type(MediaType.valueOf(stream.getFeedId().split("/")[1].toUpperCase()))
                                      .userId(stream.getFeedId().split("/")[0])
                                      .mid(stream.getMid()))
                                    .collect(Collectors.toList()))));
                          }
                        });
                      break;
                    case PUBLISHED:
                      String publishedFeedId = videoServerEvent.getEventInfo().getEventData().getId();
                      videoServerSessionRepository.getByUserId(publishedFeedId.split("/")[0])
                        .ifPresent(videoServerSession -> {
                          String meetingId = videoServerSession.getId().getMeetingId();
                          List<String> sessionUserIds = videoServerSessionRepository.getByMeetingId(meetingId)
                            .stream()
                            .map(VideoServerSession::getUserId).collect(Collectors.toList());
                          eventDispatcher.sendToUserExchange(sessionUserIds, MeetingMediaStreamChanged.create()
                            .meetingId(UUID.fromString(meetingId))
                            .userId(UUID.fromString(videoServerSession.getUserId()))
                            .mediaType(MediaType.valueOf(publishedFeedId.split("/")[1].toUpperCase()))
                            .active(PUBLISHED.equals(videoServerEvent.getEventInfo().getEventData().getEvent())));
                        });
                      break;
                    default:
                      break;
                  }
                });
              break;
            default:
              break;
          }
        });
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

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

  }
}
