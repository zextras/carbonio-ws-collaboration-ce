// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MediaType;
import com.zextras.carbonio.async.model.MeetingAudioAnswered;
import com.zextras.carbonio.async.model.MeetingMediaStreamChanged;
import com.zextras.carbonio.async.model.MeetingParticipantSubscribed;
import com.zextras.carbonio.async.model.MeetingParticipantTalking;
import com.zextras.carbonio.async.model.MeetingSdpAnswered;
import com.zextras.carbonio.async.model.MeetingSdpOffered;
import com.zextras.carbonio.async.model.SubscribedStream;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.EventData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.StreamData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaTrackType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.UserFeed;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class VideoServerEventListener {

  private static final String JANUS_EXCHANGE = "janus-exchange";
  private static final String JANUS_QUEUE = "janus-queue";
  private static final String JANUS_ROUTING_KEY = "janus-events";

  private static final String LOCAL = "local";
  private static final String TALKING = "talking";
  private static final String STOPPED_TALKING = "stopped-talking";
  private static final String SUBSCRIBING = "subscribing";
  private static final String UPDATED = "updated";
  private static final String PUBLISHED = "published";

  private static final int JSEP_TYPE = 8;
  private static final int PLUGIN_TYPE = 64;

  private volatile String consumerTag;

  private final Channel channel;
  private final EventDispatcher eventDispatcher;
  private final ObjectMapper objectMapper;
  private final VideoServerService videoServerService;

  @Inject
  public VideoServerEventListener(
      Channel channel,
      EventDispatcher eventDispatcher,
      ObjectMapper objectMapper,
      VideoServerService videoServerService) {
    this.channel = getRecoverableChannel(channel);
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
    this.videoServerService = videoServerService;
    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::stop, "Video server event listener shutdown hook"));
  }

  private @NotNull Channel getRecoverableChannel(Channel channel) {
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
    return channel;
  }

  public void start() {
    if (channel == null || !channel.isOpen()) {
      throw new EventDispatcherException("Video server event listener channel is not up!");
    }
    try {
      channel.exchangeDeclare(JANUS_EXCHANGE, BuiltinExchangeType.DIRECT, false);
      channel.queueDeclare(JANUS_QUEUE, false, false, false, null);
      channel.queueBind(JANUS_QUEUE, JANUS_EXCHANGE, JANUS_ROUTING_KEY);
      DeliverCallback deliverCallback = createDeliveryCallBack();
      consumerTag = channel.basicConsume(JANUS_QUEUE, true, deliverCallback, tag -> {});
    } catch (Exception e) {
      ChatsLogger.error("Error starting video server events processing", e);
    }
  }

  public void stop() {
    try {
      if (channel != null && channel.isOpen()) {
        if (consumerTag != null) {
          channel.basicCancel(consumerTag);
          ChatsLogger.info("Video server event listener consumer canceled successfully.");
        }
        channel.close();
        ChatsLogger.info("Video server event listener channel closed successfully.");
      }
    } catch (Exception e) {
      ChatsLogger.error("Error during stopping video server event listener", e);
    }
  }

  private @NotNull DeliverCallback createDeliveryCallBack() {
    return (tag, delivery) -> {
      String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
      VideoServerEvent event = objectMapper.readValue(message, VideoServerEvent.class);

      switch (event.getType()) {
        case JSEP_TYPE -> handleJsepTypeEvent(event);
        case PLUGIN_TYPE -> handlePluginTypeEvent(event);
        default -> {
          /* just ignore other events */
        }
      }
    };
  }

  private void handlePluginTypeEvent(VideoServerEvent event) {
    EventData data = event.getEventInfo().getEventData();

    if (data.getAudioBridge() != null) {
      handleAudioBridgeEvent(event);
    }

    if (data.getEvent() != null) {
      handleStreamsEvent(event);
    }
  }

  private void handleJsepTypeEvent(VideoServerEvent event) {
    String owner = event.getEventInfo().getOwner();
    if (!LOCAL.equalsIgnoreCase(owner)) return;

    UserFeed userFeed = UserFeed.fromString(event.getOpaqueId());
    RtcSessionDescription rtc = event.getEventInfo().getRtcSessionDescription();

    if (rtc == null) return;

    UUID meetingId = UUID.fromString(userFeed.getMeetingId());
    UUID userId = UUID.fromString(userFeed.getUserId());
    MediaType mediaType = mapEventType(userFeed.getMediaTrackType());

    switch (rtc.getType()) {
      case OFFER ->
          eventDispatcher.sendToUserExchange(
              userFeed.getUserId(),
              MeetingSdpOffered.create()
                  .meetingId(meetingId)
                  .userId(userId)
                  .mediaType(mediaType)
                  .sdp(rtc.getSdp())
                  .type(EventType.MEETING_SDP_OFFERED)
                  .sentDate(OffsetDateTime.now()));

      case ANSWER -> dispatchAnswerEvent(userFeed, rtc.getSdp(), mediaType);
    }
  }

  private void dispatchAnswerEvent(UserFeed userFeed, String sdp, MediaType mediaType) {
    UUID meetingId = UUID.fromString(userFeed.getMeetingId());
    UUID userId = UUID.fromString(userFeed.getUserId());

    switch (mediaType) {
      case AUDIO ->
          eventDispatcher.sendToUserExchange(
              userFeed.getUserId(),
              MeetingAudioAnswered.create()
                  .meetingId(meetingId)
                  .userId(userId)
                  .sdp(sdp)
                  .type(EventType.MEETING_AUDIO_ANSWERED)
                  .sentDate(OffsetDateTime.now()));

      case VIDEO, SCREEN ->
          eventDispatcher.sendToUserExchange(
              userFeed.getUserId(),
              MeetingSdpAnswered.create()
                  .meetingId(meetingId)
                  .userId(userId)
                  .mediaType(mediaType)
                  .sdp(sdp)
                  .type(EventType.MEETING_SDP_ANSWERED)
                  .sentDate(OffsetDateTime.now()));
    }
  }

  private void handleAudioBridgeEvent(VideoServerEvent event) {
    String audioBridgeEvent = event.getEventInfo().getEventData().getAudioBridge();
    if (!TALKING.equals(audioBridgeEvent) && !STOPPED_TALKING.equals(audioBridgeEvent)) return;

    UserFeed userFeed = UserFeed.fromString(event.getOpaqueId());
    List<String> recipients = getMeetingVideoServerSessions(userFeed.getMeetingId());

    eventDispatcher.sendToUserExchange(
        recipients,
        MeetingParticipantTalking.create()
            .meetingId(UUID.fromString(userFeed.getMeetingId()))
            .userId(UUID.fromString(userFeed.getUserId()))
            .isTalking(TALKING.equals(audioBridgeEvent))
            .type(EventType.MEETING_PARTICIPANT_TALKING)
            .sentDate(OffsetDateTime.now()));
  }

  private void handleStreamsEvent(VideoServerEvent event) {
    EventData data = event.getEventInfo().getEventData();
    switch (data.getEvent()) {
      case SUBSCRIBING, UPDATED -> handleUpdatedEvent(event);
      case PUBLISHED -> handlePublishedEvent(event);
      default -> {
        /* just ignore other events */
      }
    }
  }

  private void handleUpdatedEvent(VideoServerEvent event) {
    List<StreamData> streams =
        Optional.ofNullable(event.getEventInfo().getEventData().getStreamList())
            .orElse(Collections.emptyList())
            .stream()
            .filter(s -> s.getFeedId() != null)
            .toList();

    UserFeed userFeed = UserFeed.fromString(event.getOpaqueId());

    if (List.of(MediaTrackType.VIDEO_OUT, MediaTrackType.SCREEN)
            .contains(userFeed.getMediaTrackType())
        && !streams.isEmpty()) {
      return;
    }

    eventDispatcher.sendToUserExchange(
        userFeed.getUserId(),
        MeetingParticipantSubscribed.create()
            .meetingId(UUID.fromString(userFeed.getMeetingId()))
            .userId(UUID.fromString(userFeed.getUserId()))
            .streams(
                streams.stream()
                    .map(
                        s -> {
                          Feed f = Feed.fromString(s.getFeedId());
                          return SubscribedStream.create()
                              .type(f.getType().toString().toLowerCase())
                              .userId(UUID.fromString(f.getUserId()))
                              .mid(s.getMid());
                        })
                    .toList())
            .type(EventType.MEETING_PARTICIPANT_SUBSCRIBED)
            .sentDate(OffsetDateTime.now()));
  }

  private void handlePublishedEvent(VideoServerEvent event) {
    UserFeed userFeed = UserFeed.fromString(event.getOpaqueId());
    MediaType mediaType = mapEventType(userFeed.getMediaTrackType());

    eventDispatcher.sendToUserExchange(
        getMeetingVideoServerSessions(userFeed.getMeetingId()),
        MeetingMediaStreamChanged.create()
            .meetingId(UUID.fromString(userFeed.getMeetingId()))
            .userId(UUID.fromString(userFeed.getUserId()))
            .mediaType(mediaType)
            .active(PUBLISHED.equals(event.getEventInfo().getEventData().getEvent()))
            .type(EventType.MEETING_MEDIA_STREAM_CHANGED)
            .sentDate(OffsetDateTime.now()));
  }

  private @NotNull List<String> getMeetingVideoServerSessions(String meetingId) {
    return videoServerService.getSessions(meetingId).stream()
        .map(VideoServerSession::getUserId)
        .toList();
  }

  private MediaType mapEventType(MediaTrackType mediaTrackType) {
    return switch (mediaTrackType) {
      case AUDIO -> MediaType.AUDIO;
      case VIDEO_OUT, VIDEO_IN -> MediaType.VIDEO;
      case SCREEN -> MediaType.SCREEN;
    };
  }
}
