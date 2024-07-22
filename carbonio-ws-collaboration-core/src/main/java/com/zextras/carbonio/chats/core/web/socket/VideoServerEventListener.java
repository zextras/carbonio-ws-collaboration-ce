// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.event.MeetingAudioAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingMediaStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantSubscribed;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantTalking;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpAnswered;
import com.zextras.carbonio.chats.core.data.event.MeetingSdpOffered;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.StreamData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.SubscribedStream;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.vavr.MatchError;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class VideoServerEventListener {

  private static final String JANUS_EXCHANGE = "janus-exchange";
  private static final String JANUS_EVENTS = "janus-events";

  private static final String LOCAL = "local";
  private static final String TALKING = "talking";
  private static final String STOPPED_TALKING = "stopped-talking";
  private static final String SUBSCRIBING_TYPE_EVENT = "subscribing";
  private static final String UPDATED_TYPE_EVENT = "updated";
  private static final String PUBLISHED = "published";

  private static final int JSEP_TYPE = 8;
  private static final int PLUGIN_TYPE = 64;

  private final Channel channel;
  private final EventDispatcher eventDispatcher;
  private final ObjectMapper objectMapper;
  private final VideoServerService videoServerService;

  private enum EventType {
    AUDIO,
    VIDEOIN,
    VIDEOOUT,
    SCREEN
  }

  @Inject
  public VideoServerEventListener(
      Channel channel,
      EventDispatcher eventDispatcher,
      ObjectMapper objectMapper,
      VideoServerService videoServerService) {
    Recoverable channelRecoverable = (Recoverable) channel;
    channelRecoverable.addRecoveryListener(
        new RecoveryListener() {
          @Override
          public void handleRecovery(Recoverable recoverable) {
            ChatsLogger.warn("Videoserver event listener channel recovery completed successfully");
            start();
          }

          @Override
          public void handleRecoveryStarted(Recoverable recoverable) {
            ChatsLogger.warn("Videoserver event listener channel recovery started...");
          }
        });
    this.channel = channel;
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
    this.videoServerService = videoServerService;
  }

  public void start() {
    if (channel == null || !channel.isOpen()) {
      throw new EventDispatcherException("Video server event listener channel is not up!");
    }
    try {
      channel.queueDeclare(JANUS_EVENTS, false, false, false, null);
      channel.exchangeDeclare(JANUS_EXCHANGE, "direct");
      channel.queueBind(JANUS_EVENTS, JANUS_EXCHANGE, JANUS_EVENTS);
      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            VideoServerEvent videoServerEvent =
                objectMapper.readValue(message, VideoServerEvent.class);
            switch (videoServerEvent.getType()) {
              case JSEP_TYPE:
                handleJsepTypeEvent(videoServerEvent);
                break;
              case PLUGIN_TYPE:
                handleAudioBridgeEvent(videoServerEvent);
                handleStreamsEvent(videoServerEvent);
                break;
              default:
                break;
            }
          };
      channel.basicConsume(JANUS_EVENTS, true, deliverCallback, consumerTag -> this.start());
    } catch (Exception e) {
      throw new VideoServerException("Error during processing video server events ", e);
    }
  }

  private void handleStreamsEvent(VideoServerEvent videoServerEvent) {
    Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getEvent())
        .ifPresent(
            eventType -> {
              switch (eventType) {
                case UPDATED_TYPE_EVENT, SUBSCRIBING_TYPE_EVENT:
                  Optional.ofNullable(
                          videoServerEvent.getEventInfo().getEventData().getStreamList())
                      .ifPresent(
                          streamData -> {
                            List<StreamData> streamDataList =
                                streamData.stream()
                                    .filter(
                                        stream ->
                                            Optional.ofNullable(stream.getFeedId()).isPresent())
                                    .toList();
                            if (!streamDataList.isEmpty()) {
                              videoServerService
                                  .getSession(String.valueOf(videoServerEvent.getSessionId()))
                                  .ifPresent(
                                      videoServerSession ->
                                          eventDispatcher.sendToUserExchange(
                                              videoServerSession.getUserId(),
                                              MeetingParticipantSubscribed.create()
                                                  .meetingId(
                                                      UUID.fromString(
                                                          videoServerSession
                                                              .getId()
                                                              .getMeetingId()))
                                                  .userId(
                                                      UUID.fromString(
                                                          videoServerSession.getUserId()))
                                                  .streams(
                                                      streamDataList.stream()
                                                          .map(
                                                              stream -> {
                                                                Feed feed =
                                                                    Feed.fromString(
                                                                        stream.getFeedId());
                                                                return SubscribedStream.create()
                                                                    .type(
                                                                        feed.getType()
                                                                            .toString()
                                                                            .toLowerCase())
                                                                    .userId(feed.getUserId())
                                                                    .mid(stream.getMid());
                                                              })
                                                          .toList())));
                            }
                          });
                  break;
                case PUBLISHED:
                  Feed feed =
                      Feed.fromString(videoServerEvent.getEventInfo().getEventData().getId());
                  String meetingId =
                      videoServerEvent.getEventInfo().getEventData().getRoom().split("_")[1];
                  List<String> sessionUserIds =
                      videoServerService.getSessions(meetingId).stream()
                          .map(VideoServerSession::getUserId)
                          .toList();
                  eventDispatcher.sendToUserExchange(
                      sessionUserIds,
                      MeetingMediaStreamChanged.create()
                          .meetingId(UUID.fromString(meetingId))
                          .userId(UUID.fromString(feed.getUserId()))
                          .mediaType(feed.getType())
                          .active(
                              PUBLISHED.equals(
                                  videoServerEvent.getEventInfo().getEventData().getEvent())));
                  break;
                default:
                  break;
              }
            });
  }

  private void handleAudioBridgeEvent(VideoServerEvent videoServerEvent) {
    Optional.ofNullable(videoServerEvent.getEventInfo().getEventData().getAudioBridge())
        .ifPresent(
            eventType -> {
              if (TALKING.equals(eventType) || STOPPED_TALKING.equals(eventType)) {
                String meetingId =
                    videoServerEvent.getEventInfo().getEventData().getRoom().split("_")[1];
                List<String> sessionUserIds =
                    videoServerService.getSessions(meetingId).stream()
                        .map(VideoServerSession::getUserId)
                        .toList();
                eventDispatcher.sendToUserExchange(
                    sessionUserIds,
                    MeetingParticipantTalking.create()
                        .meetingId(UUID.fromString(meetingId))
                        .userId(
                            UUID.fromString(videoServerEvent.getEventInfo().getEventData().getId()))
                        .isTalking(
                            TALKING.equals(
                                videoServerEvent.getEventInfo().getEventData().getAudioBridge())));
              }
            });
  }

  private void handleJsepTypeEvent(VideoServerEvent videoServerEvent) {
    Optional.ofNullable(videoServerEvent.getEventInfo().getOwner())
        .ifPresent(
            owner -> {
              if (LOCAL.equalsIgnoreCase(owner)) {
                videoServerService
                    .getSession(String.valueOf(videoServerEvent.getSessionId()))
                    .ifPresent(
                        videoServerSession -> {
                          try {
                            EventType eventType =
                                Match(videoServerEvent.getHandleId().toString())
                                    .of(
                                        Case(
                                            $(videoServerSession.getAudioHandleId()),
                                            EventType.AUDIO),
                                        Case(
                                            $(videoServerSession.getVideoInHandleId()),
                                            EventType.VIDEOIN),
                                        Case(
                                            $(videoServerSession.getVideoOutHandleId()),
                                            EventType.VIDEOOUT),
                                        Case(
                                            $(videoServerSession.getScreenHandleId()),
                                            EventType.SCREEN));
                            RtcSessionDescription rtcSessionDescription =
                                videoServerEvent.getEventInfo().getRtcSessionDescription();
                            switch (rtcSessionDescription.getType()) {
                              case OFFER:
                                eventDispatcher.sendToUserExchange(
                                    videoServerSession.getUserId(),
                                    MeetingSdpOffered.create()
                                        .meetingId(
                                            UUID.fromString(
                                                videoServerSession.getId().getMeetingId()))
                                        .userId(UUID.fromString(videoServerSession.getUserId()))
                                        .mediaType(
                                            eventType == EventType.SCREEN
                                                ? MediaType.SCREEN
                                                : MediaType.VIDEO)
                                        .sdp(rtcSessionDescription.getSdp()));
                                break;
                              case ANSWER:
                                switch (eventType) {
                                  case AUDIO:
                                    eventDispatcher.sendToUserExchange(
                                        videoServerSession.getUserId(),
                                        MeetingAudioAnswered.create()
                                            .meetingId(
                                                UUID.fromString(
                                                    videoServerSession.getId().getMeetingId()))
                                            .userId(UUID.fromString(videoServerSession.getUserId()))
                                            .sdp(rtcSessionDescription.getSdp()));
                                    break;
                                  case VIDEOIN, VIDEOOUT, SCREEN:
                                    eventDispatcher.sendToUserExchange(
                                        videoServerSession.getUserId(),
                                        MeetingSdpAnswered.create()
                                            .meetingId(
                                                UUID.fromString(
                                                    videoServerSession.getId().getMeetingId()))
                                            .userId(UUID.fromString(videoServerSession.getUserId()))
                                            .mediaType(
                                                eventType == EventType.SCREEN
                                                    ? MediaType.SCREEN
                                                    : MediaType.VIDEO)
                                            .sdp(rtcSessionDescription.getSdp()));
                                    break;
                                  default:
                                    break;
                                }
                                break;
                              default:
                                break;
                            }
                          } catch (MatchError m) {
                            throw new VideoServerException(
                                "Invalid event handle id: " + m.getObject());
                          }
                        });
              }
            });
  }
}
