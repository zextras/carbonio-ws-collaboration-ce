// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MessageBrokerVideoserverHealthMonitorTest {

  private static final String RESTORED = "MessageBrokerRestored";
  private static final String DISCONNECTED = "MessageBrokerDisconnected";

  private final EventWebSocketSessions sessions = mock(EventWebSocketSessions.class);
  private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private Instant now = Instant.parse("2026-11-13T10:00:00Z");
  private MessageBrokerVideoserverHealthMonitor monitor;

  @BeforeEach
  void setUp() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenAnswer(invocation -> now);
    monitor = new MessageBrokerVideoserverHealthMonitor(sessions, objectMapper, clock, scheduler);
  }

  private void elapse(Duration duration) {
    now = now.plus(duration);
  }

  private void runHeartbeatCheck() {
    ArgumentCaptor<Runnable> check = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler)
        .scheduleWithFixedDelay(check.capture(), eq(5_000L), eq(5_000L), eq(TimeUnit.MILLISECONDS));
    check.getValue().run();
  }

  private List<JsonNode> broadcastEvents() {
    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(sessions, atLeast(0)).broadcast(json.capture());
    return json.getAllValues().stream().map(this::parse).toList();
  }

  private List<String> broadcastTypes() {
    return broadcastEvents().stream().map(event -> event.get("type").asText()).toList();
  }

  private JsonNode parse(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  void beforeTheFirstHeartbeat_meetingMediaIsRefused() {
    assertThrows(VideoServerException.class, () -> monitor.checkStatus());
    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void firstHeartbeat_allowsMeetingMedia_andBroadcastsRestored() {
    monitor.notifyVideoServerReady();

    assertDoesNotThrow(() -> monitor.checkStatus());
    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void furtherHeartbeats_broadcastNothingMore() {
    monitor.notifyVideoServerReady();
    monitor.notifyVideoServerReady();
    monitor.notifyVideoServerReady();

    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void heartbeatWithinTheLastMinute_keepsMeetingMediaAllowed() {
    monitor.notifyVideoServerReady();
    elapse(Duration.ofSeconds(60));

    runHeartbeatCheck();

    assertDoesNotThrow(() -> monitor.checkStatus());
    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void noHeartbeatForOverAMinute_broadcastsDisconnected_andRefusesMeetingMedia() {
    monitor.notifyVideoServerReady();
    elapse(Duration.ofSeconds(61));

    runHeartbeatCheck();

    assertThrows(VideoServerException.class, () -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void staleHeartbeatCheckedAgain_broadcastsDisconnectedOnce() {
    monitor.notifyVideoServerReady();
    elapse(Duration.ofSeconds(61));

    runHeartbeatCheck();
    runHeartbeatCheck();

    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void heartbeatAfterItWentStale_allowsMeetingMediaAgain_andBroadcastsRestored() {
    monitor.notifyVideoServerReady();
    elapse(Duration.ofSeconds(61));
    runHeartbeatCheck();

    monitor.notifyVideoServerReady();
    runHeartbeatCheck();

    assertDoesNotThrow(() -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void brokerDown_broadcastsDisconnectedAtOnce_andRefusesMeetingMedia() {
    monitor.notifyVideoServerReady();

    monitor.notifyBrokerDown();

    assertThrows(VideoServerException.class, () -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void brokerDownWhileMeetingMediaIsAlreadyRefused_broadcastsNothing() {
    monitor.notifyBrokerDown();
    monitor.notifyBrokerDown();

    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void heartbeatWhileTheBrokerConnectionIsDown_keepsMeetingMediaRefused() {
    monitor.notifyVideoServerReady();
    monitor.notifyBrokerDown();

    monitor.notifyVideoServerReady();

    assertThrows(VideoServerException.class, () -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void brokerRecovered_afterAHeartbeatSinceTheDrop_allowsMeetingMedia_andBroadcastsRestored() {
    monitor.notifyVideoServerReady();
    monitor.notifyBrokerDown();
    monitor.notifyVideoServerReady();

    monitor.notifyBrokerRecovered();

    assertDoesNotThrow(() -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void brokerRecovered_withoutAHeartbeatSinceTheDrop_waitsForTheNextHeartbeat() {
    monitor.notifyVideoServerReady();
    monitor.notifyBrokerDown();

    monitor.notifyBrokerRecovered();

    assertThrows(VideoServerException.class, () -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());

    monitor.notifyVideoServerReady();

    assertDoesNotThrow(() -> monitor.checkStatus());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void broadcastEvents_carryTheCurrentSentDate() {
    OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

    monitor.notifyVideoServerReady();
    monitor.notifyBrokerDown();

    broadcastEvents()
        .forEach(
            event ->
                assertTrue(OffsetDateTime.parse(event.get("sentDate").asText()).isAfter(before)));
  }

  private RemoteEndpoint.Async openedSession() {
    Session session = mock(Session.class);
    RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
    when(session.getAsyncRemote()).thenReturn(remote);
    monitor.notifyIfUnavailable(session);
    return remote;
  }

  @Test
  void sessionOpenedWhileMeetingMediaIsRefused_isToldDisconnected() {
    RemoteEndpoint.Async remote = openedSession();

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(remote).sendText(json.capture());
    assertEquals(DISCONNECTED, parse(json.getValue()).get("type").asText());
    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void sessionOpenedWhileMeetingMediaIsAllowed_isToldNothing() {
    monitor.notifyVideoServerReady();

    RemoteEndpoint.Async remote = openedSession();

    verify(remote, never()).sendText(anyString());
  }
}
