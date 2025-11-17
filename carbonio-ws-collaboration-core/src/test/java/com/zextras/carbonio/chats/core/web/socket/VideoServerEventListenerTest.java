package com.zextras.carbonio.chats.core.web.socket;

// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.Recoverable;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MediaType;
import com.zextras.carbonio.async.model.MeetingAudioAnswered;
import com.zextras.carbonio.async.model.MeetingMediaStreamChanged;
import com.zextras.carbonio.async.model.MeetingParticipantSubscribed;
import com.zextras.carbonio.async.model.MeetingParticipantTalking;
import com.zextras.carbonio.async.model.MeetingSdpAnswered;
import com.zextras.carbonio.async.model.MeetingSdpOffered;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.EventData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.EventInfo;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.StreamData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event.VideoServerEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcType;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VideoServerEventListenerTest {

  private static final String JANUS_EXCHANGE = "janus-exchange";
  private static final String JANUS_QUEUE = "janus-queue";
  private static final String JANUS_ROUTING_KEY = "janus-events";
  private static final String CONSUMER_TAG = "consumer-tag-123";

  private Channel channel;
  private EventDispatcher eventDispatcher;
  private VideoServerService videoServerService;
  private ArgumentCaptor<DeliverCallback> deliverCallbackCaptor;
  private ObjectMapper objectMapper;
  private VideoServerEventListener listener;

  @BeforeEach
  void setUp() throws Exception {
    channel = mock(Channel.class, withSettings().extraInterfaces(Recoverable.class));
    eventDispatcher = mock(EventDispatcher.class);
    videoServerService = mock(VideoServerService.class);
    deliverCallbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
    objectMapper = new ObjectMapper();

    when(channel.isOpen()).thenReturn(true);
    when(channel.basicConsume(
            anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class)))
        .thenReturn(CONSUMER_TAG);

    listener =
        new VideoServerEventListener(channel, eventDispatcher, objectMapper, videoServerService);
  }

  @Test
  void testStartSuccessfully() throws Exception {
    listener.start();

    verify(channel).exchangeDeclare(JANUS_EXCHANGE, BuiltinExchangeType.DIRECT, false);
    verify(channel).queueDeclare(JANUS_QUEUE, false, false, false, null);
    verify(channel).queueBind(JANUS_QUEUE, JANUS_EXCHANGE, JANUS_ROUTING_KEY);
    verify(channel)
        .basicConsume(
            eq(JANUS_QUEUE), eq(true), any(DeliverCallback.class), any(CancelCallback.class));
  }

  @Test
  void testStartWithClosedChannel() {
    when(channel.isOpen()).thenReturn(false);
    assertThrows(EventDispatcherException.class, () -> listener.start());
  }

  @Test
  void testStartWithNullChannel() {
    VideoServerEventListener listenerWithNullChannel =
        new VideoServerEventListener(null, eventDispatcher, objectMapper, videoServerService);
    assertThrows(EventDispatcherException.class, listenerWithNullChannel::start);
  }

  @Test
  void testStopSuccessfully() throws Exception {
    listener.start();
    listener.stop();

    verify(channel).basicCancel(CONSUMER_TAG);
    verify(channel).close();
  }

  @Test
  void testHandleJsepOfferEvent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    String sdp = "v=0\r\no=- 123456 0 IN IP4 127.0.0.1\r\n";

    VideoServerEvent event = createJsepEvent("vo", userId, meetingId, RtcType.OFFER, sdp);

    MeetingSdpOffered capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingSdpOffered.class);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertEquals(MediaType.VIDEO, capturedEvent.getMediaType());
    assertEquals(sdp, capturedEvent.getSdp());
    assertEquals(EventType.MEETING_SDP_OFFERED, capturedEvent.getType());
  }

  @Test
  void testHandleJsepAnswerEventAudio() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    String sdp = "v=0\r\no=- 123456 0 IN IP4 127.0.0.1\r\n";

    VideoServerEvent event = createJsepEvent("a", userId, meetingId, RtcType.ANSWER, sdp);

    MeetingAudioAnswered capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingAudioAnswered.class);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertEquals(sdp, capturedEvent.getSdp());
    assertEquals(EventType.MEETING_AUDIO_ANSWERED, capturedEvent.getType());
  }

  @Test
  void testHandleJsepAnswerEventVideo() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    String sdp = "v=0\r\no=- 123456 0 IN IP4 127.0.0.1\r\n";

    VideoServerEvent event = createJsepEvent("vo", userId, meetingId, RtcType.ANSWER, sdp);

    MeetingSdpAnswered capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingSdpAnswered.class);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertEquals(MediaType.VIDEO, capturedEvent.getMediaType());
    assertEquals(sdp, capturedEvent.getSdp());
    assertEquals(EventType.MEETING_SDP_ANSWERED, capturedEvent.getType());
  }

  @Test
  void testHandleAudioBridgeTalkingEvent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    List<VideoServerSession> sessions =
        Arrays.asList(
            createVideoServerSession(userId.toString()),
            createVideoServerSession(UUID.randomUUID().toString()));
    when(videoServerService.getSessions(meetingId.toString())).thenReturn(sessions);

    VideoServerEvent event = createAudioBridgeEvent("a", userId, meetingId, "talking");

    MeetingParticipantTalking capturedEvent =
        handleEventAndCaptureList(event, MeetingParticipantTalking.class, 2);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertTrue(capturedEvent.isIsTalking());
    assertEquals(EventType.MEETING_PARTICIPANT_TALKING, capturedEvent.getType());
  }

  @Test
  void testHandleAudioBridgeStoppedTalkingEvent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    when(videoServerService.getSessions(meetingId.toString()))
        .thenReturn(Collections.singletonList(createVideoServerSession(userId.toString())));

    VideoServerEvent event = createAudioBridgeEvent("a", userId, meetingId, "stopped-talking");

    MeetingParticipantTalking capturedEvent =
        handleEventAndCaptureList(event, MeetingParticipantTalking.class, 1);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertFalse(capturedEvent.isIsTalking());
    assertEquals(EventType.MEETING_PARTICIPANT_TALKING, capturedEvent.getType());
  }

  @Test
  void testHandlePublishedEvent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    when(videoServerService.getSessions(meetingId.toString()))
        .thenReturn(Collections.singletonList(createVideoServerSession(userId.toString())));

    VideoServerEvent event = createPluginEvent("vo", userId, meetingId, "published", null);

    MeetingMediaStreamChanged capturedEvent =
        handleEventAndCaptureList(event, MeetingMediaStreamChanged.class, 1);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertEquals(MediaType.VIDEO, capturedEvent.getMediaType());
    assertTrue(capturedEvent.isActive());
    assertEquals(EventType.MEETING_MEDIA_STREAM_CHANGED, capturedEvent.getType());
  }

  @Test
  void testHandleSubscribingEventForVideoIn() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    String feedId = String.format("%s/video", otherUserId);

    VideoServerEvent event =
        createPluginEvent(
            "vi",
            userId,
            meetingId,
            "subscribing",
            Collections.singletonList(createStreamData(feedId, "mid-123")));

    MeetingParticipantSubscribed capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingParticipantSubscribed.class);

    assertEquals(meetingId, capturedEvent.getMeetingId());
    assertEquals(userId, capturedEvent.getUserId());
    assertEquals(1, capturedEvent.getStreams().size());
    assertEquals(EventType.MEETING_PARTICIPANT_SUBSCRIBED, capturedEvent.getType());
  }

  @Test
  void testHandleUpdatedEventWithNullStreamList() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event = createPluginEvent("vi", userId, meetingId, "updated", null);

    MeetingParticipantSubscribed capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingParticipantSubscribed.class);

    assertTrue(capturedEvent.getStreams().isEmpty());
  }

  @Test
  void testHandleUpdatedEventWithEmptyStreamList() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent("vi", userId, meetingId, "updated", Collections.emptyList());

    MeetingParticipantSubscribed capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingParticipantSubscribed.class);

    assertTrue(capturedEvent.getStreams().isEmpty());
  }

  @Test
  void testHandleUpdatedEventWithStreamListContainingNullFeedIds() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent(
            "vi",
            userId,
            meetingId,
            "updated",
            Collections.singletonList(createStreamData(null, "mid-123")));

    MeetingParticipantSubscribed capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingParticipantSubscribed.class);

    assertTrue(capturedEvent.getStreams().isEmpty());
  }

  @Test
  void testHandleUpdatedEventWithMultipleValidStreams() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID otherUserId1 = UUID.randomUUID();
    UUID otherUserId2 = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent(
            "vi",
            userId,
            meetingId,
            "updated",
            Arrays.asList(
                createStreamData(String.format("%s/video", otherUserId1), "mid-1"),
                createStreamData(String.format("%s/screen", otherUserId2), "mid-2")));

    MeetingParticipantSubscribed capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingParticipantSubscribed.class);

    assertEquals(2, capturedEvent.getStreams().size());
    assertEquals("mid-1", capturedEvent.getStreams().get(0).getMid());
    assertEquals("mid-2", capturedEvent.getStreams().get(1).getMid());
  }

  @Test
  void testHandleUpdatedEventWithMixedValidAndNullFeedIds() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent(
            "vi",
            userId,
            meetingId,
            "updated",
            Arrays.asList(
                createStreamData(String.format("%s/video", otherUserId), "mid-valid"),
                createStreamData(null, "mid-invalid")));

    MeetingParticipantSubscribed capturedEvent =
        handleEventAndCapture(event, userId.toString(), MeetingParticipantSubscribed.class);

    assertEquals(1, capturedEvent.getStreams().size());
    assertEquals("mid-valid", capturedEvent.getStreams().get(0).getMid());
  }

  @Test
  void testHandleUpdatedEventWithNonVideoInMediaType() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent(
            "a",
            userId,
            meetingId,
            "updated",
            Collections.singletonList(
                createStreamData(String.format("%s/video", otherUserId), "mid-123")));

    handleEvent(event);

    verify(eventDispatcher, never())
        .sendToUserExchange(eq(userId.toString()), any(MeetingParticipantSubscribed.class));
  }

  @Test
  void testHandleUpdatedEventWithVideoOutMediaType() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent(
            "vo",
            userId,
            meetingId,
            "updated",
            Collections.singletonList(
                createStreamData(String.format("%s/video", otherUserId), "mid-123")));

    handleEvent(event);

    verify(eventDispatcher, never())
        .sendToUserExchange(eq(userId.toString()), any(MeetingParticipantSubscribed.class));
  }

  @Test
  void testHandleUpdatedEventWithScreenMediaType() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event =
        createPluginEvent(
            "s",
            userId,
            meetingId,
            "updated",
            Collections.singletonList(
                createStreamData(String.format("%s/video", otherUserId), "mid-123")));

    handleEvent(event);

    verify(eventDispatcher, never())
        .sendToUserExchange(eq(userId.toString()), any(MeetingParticipantSubscribed.class));
  }

  @Test
  void testIgnoreNonLocalOwnerEvents() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    VideoServerEvent event = createJsepEvent("vo", userId, meetingId, RtcType.OFFER, "sdp");
    event.getEventInfo().owner("remote");

    handleEvent(event);

    verify(eventDispatcher, never()).sendToUserExchange(anyString(), any());
  }

  @Test
  void testIgnoreUnknownEventTypes() throws Exception {
    VideoServerEvent event = new VideoServerEvent();
    event.type(999);
    event.opaqueId("a/" + UUID.randomUUID() + "/" + UUID.randomUUID());
    event.eventInfo(new EventInfo());

    handleEvent(event);

    verify(eventDispatcher, never()).sendToUserExchange(anyString(), any());
  }

  // Helper methods
  private VideoServerEvent createJsepEvent(
      String prefix, UUID userId, UUID meetingId, RtcType sdpType, String sdp) {
    VideoServerEvent event = new VideoServerEvent();
    event.type(8);
    event.opaqueId(String.format("%s/%s/%s", prefix, userId, meetingId));

    EventInfo eventInfo = new EventInfo();
    eventInfo.owner("local");

    RtcSessionDescription rtc = new RtcSessionDescription();
    rtc.type(sdpType);
    rtc.sdp(sdp);
    eventInfo.rtcSessionDescription(rtc);

    event.eventInfo(eventInfo);
    return event;
  }

  private VideoServerEvent createAudioBridgeEvent(
      String prefix, UUID userId, UUID meetingId, String audioBridgeEvent) {
    VideoServerEvent event = new VideoServerEvent();
    event.type(64);
    event.opaqueId(String.format("%s/%s/%s", prefix, userId, meetingId));

    EventInfo eventInfo = new EventInfo();
    EventData eventData = new EventData();
    eventData.audioBridge(audioBridgeEvent);
    eventInfo.eventData(eventData);

    event.eventInfo(eventInfo);
    return event;
  }

  private VideoServerEvent createPluginEvent(
      String prefix, UUID userId, UUID meetingId, String eventType, List<StreamData> streams) {
    VideoServerEvent event = new VideoServerEvent();
    event.type(64);
    event.opaqueId(String.format("%s/%s/%s", prefix, userId, meetingId));

    EventInfo eventInfo = new EventInfo();
    EventData eventData = new EventData();
    eventData.event(eventType);
    eventData.streamList(streams);
    eventInfo.eventData(eventData);

    event.eventInfo(eventInfo);
    return event;
  }

  private StreamData createStreamData(String feedId, String mid) {
    StreamData stream = new StreamData();
    stream.feedId(feedId);
    stream.mid(mid);
    return stream;
  }

  private Delivery createDelivery(String message) {
    return new Delivery(
        new Envelope(1, false, JANUS_EXCHANGE, JANUS_ROUTING_KEY),
        new BasicProperties(),
        message.getBytes(StandardCharsets.UTF_8));
  }

  private VideoServerSession createVideoServerSession(String userId) {
    VideoServerSession session = new VideoServerSession();
    session.userId(userId);
    return session;
  }

  private void handleEvent(VideoServerEvent event) throws Exception {
    String jsonEvent = objectMapper.writeValueAsString(event);
    listener.start();
    verify(channel)
        .basicConsume(
            anyString(), anyBoolean(), deliverCallbackCaptor.capture(), any(CancelCallback.class));

    DeliverCallback callback = deliverCallbackCaptor.getValue();
    callback.handle(CONSUMER_TAG, createDelivery(jsonEvent));
  }

  private <T> T handleEventAndCapture(VideoServerEvent event, String recipient, Class<T> eventClass)
      throws Exception {
    handleEvent(event);

    ArgumentCaptor<T> eventCaptor = ArgumentCaptor.forClass(eventClass);
    verify(eventDispatcher).sendToUserExchange(eq(recipient), (DomainEvent) eventCaptor.capture());
    return eventCaptor.getValue();
  }

  private <T> T handleEventAndCaptureList(
      VideoServerEvent event, Class<T> eventClass, int expectedRecipients) throws Exception {
    handleEvent(event);

    ArgumentCaptor<List<String>> recipientsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<T> eventCaptor = ArgumentCaptor.forClass(eventClass);
    verify(eventDispatcher)
        .sendToUserExchange(recipientsCaptor.capture(), (DomainEvent) eventCaptor.capture());
    assertEquals(expectedRecipients, recipientsCaptor.getValue().size());
    return eventCaptor.getValue();
  }
}
