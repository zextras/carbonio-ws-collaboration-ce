// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
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
class VideoServerMonitorTest {

  private static final String RESTORED = "MessageBrokerRestored";
  private static final String DISCONNECTED = "MessageBrokerDisconnected";

  private final EventWebSocketSessions sessions = mock(EventWebSocketSessions.class);
  private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private Instant now = Instant.parse("2026-11-13T10:00:00Z");
  private VideoServerMonitor monitor;

  @BeforeEach
  void setUp() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenAnswer(invocation -> now);
    monitor = new VideoServerMonitor(sessions, objectMapper, clock, scheduler);
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
    assertThrows(VideoServerException.class, () -> monitor.checkAvailability());
    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void firstHeartbeat_allowsMeetingMedia_andBroadcastsRestored() {
    monitor.onHeartbeat("janus", 0);

    assertDoesNotThrow(() -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void furtherHeartbeats_broadcastNothingMore() {
    monitor.onHeartbeat("janus", 0);
    monitor.onHeartbeat("janus", 0);
    monitor.onHeartbeat("janus", 0);

    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void heartbeatWithinTheLastMinute_keepsMeetingMediaAllowed() {
    monitor.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(60));

    runHeartbeatCheck();

    assertDoesNotThrow(() -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void noHeartbeatForOverAMinute_broadcastsDisconnected_andRefusesMeetingMedia() {
    monitor.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(61));

    runHeartbeatCheck();

    assertThrows(VideoServerException.class, () -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void staleHeartbeatCheckedAgain_broadcastsDisconnectedOnce() {
    monitor.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(61));

    runHeartbeatCheck();
    runHeartbeatCheck();

    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void heartbeatAfterItWentStale_allowsMeetingMediaAgain_andBroadcastsRestored() {
    monitor.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(61));
    runHeartbeatCheck();

    monitor.onHeartbeat("janus", 0);
    runHeartbeatCheck();

    assertDoesNotThrow(() -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void brokerDown_broadcastsDisconnectedAtOnce_andRefusesMeetingMedia() {
    monitor.onHeartbeat("janus", 0);

    monitor.onBrokerLost();

    assertThrows(VideoServerException.class, () -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void brokerDownWhileMeetingMediaIsAlreadyRefused_broadcastsNothing() {
    monitor.onBrokerLost();
    monitor.onBrokerLost();

    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void heartbeatWhileTheBrokerConnectionIsDown_keepsMeetingMediaRefused() {
    monitor.onHeartbeat("janus", 0);
    monitor.onBrokerLost();

    monitor.onHeartbeat("janus", 0);

    assertThrows(VideoServerException.class, () -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void brokerRecovered_afterAHeartbeatSinceTheDrop_allowsMeetingMedia_andBroadcastsRestored() {
    monitor.onHeartbeat("janus", 0);
    monitor.onBrokerLost();
    monitor.onHeartbeat("janus", 0);

    monitor.onBrokerRecovered();

    assertDoesNotThrow(() -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void brokerRecovered_withoutAHeartbeatSinceTheDrop_waitsForTheNextHeartbeat() {
    monitor.onHeartbeat("janus", 0);
    monitor.onBrokerLost();

    monitor.onBrokerRecovered();

    assertThrows(VideoServerException.class, () -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());

    monitor.onHeartbeat("janus", 0);

    assertDoesNotThrow(() -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void broadcastEvents_carryTheClockTimeAsSentDate() {
    monitor.onHeartbeat("janus", 0);
    monitor.onBrokerLost();

    broadcastEvents()
        .forEach(
            event ->
                assertEquals(
                    now, OffsetDateTime.parse(event.get("sentDate").asText()).toInstant()));
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
    monitor.onHeartbeat("janus", 0);

    RemoteEndpoint.Async remote = openedSession();

    verify(remote, never()).sendText(anyString());
  }

  @Test
  void oneVideoServerStillSendingHeartbeats_keepsMeetingMediaAllowed() {
    monitor.onHeartbeat("vs-1", 0);
    monitor.onHeartbeat("vs-2", 0);
    elapse(Duration.ofSeconds(45));
    monitor.onHeartbeat("vs-2", 0);
    elapse(Duration.ofSeconds(30));
    runHeartbeatCheck();

    assertDoesNotThrow(() -> monitor.checkAvailability());
    assertFalse(monitor.isAvailable("vs-1"));
    assertTrue(monitor.isAvailable("vs-2"));
  }

  @Test
  void aVideoServerThatNeverNamedItself_isAvailableWhileAnyHeartbeatArrives() {
    assertFalse(monitor.isAvailable("vs-1"));

    monitor.onHeartbeat("", 0);

    assertTrue(monitor.isAvailable("vs-1"));
  }

  @Test
  void brokerDown_makesEveryVideoServerUnavailable() {
    monitor.onHeartbeat("vs-1", 0);

    monitor.onBrokerLost();
    monitor.onBrokerRecovered();

    assertFalse(monitor.isAvailable("vs-1"));
  }

  @Test
  void shutdownOfTheOnlyVideoServer_refusesMeetingMediaAtOnce_andBroadcastsDisconnected() {
    monitor.onHeartbeat("vs-1", 100);

    monitor.onShutdown("vs-1", 200);

    assertFalse(monitor.isAvailable("vs-1"));
    assertThrows(VideoServerException.class, () -> monitor.checkAvailability());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void heartbeatAfterAShutdown_makesTheVideoServerAvailableAgain() {
    monitor.onHeartbeat("vs-1", 100);
    monitor.onShutdown("vs-1", 200);

    monitor.onHeartbeat("vs-1", 300);

    assertTrue(monitor.isAvailable("vs-1"));
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void shutdownRelayedAfterANewerHeartbeat_isIgnored() {
    monitor.onHeartbeat("vs-1", 300);

    monitor.onShutdown("vs-1", 200);

    assertTrue(monitor.isAvailable("vs-1"));
  }

  @Test
  void heartbeatRelayedWithoutATimestampByAnOlderInstance_isStillApplied() {
    monitor.onShutdown("vs-1", 200);

    monitor.onHeartbeat("vs-1", 0);

    assertTrue(monitor.isAvailable("vs-1"));
  }

  @Test
  void aVideoServerNeverHeardFrom_isNotDown() {
    assertFalse(monitor.isDown("vs-1"));
    assertFalse(monitor.isDown());
    assertFalse(monitor.isShutDown("vs-1"));
    assertFalse(monitor.isShutDown());
  }

  @Test
  void aShutDownVideoServer_isDownAndShutDown_untilItsNextHeartbeat() {
    monitor.onHeartbeat("vs-1", 100);
    monitor.onShutdown("vs-1", 200);

    assertTrue(monitor.isDown("vs-1"));
    assertTrue(monitor.isShutDown("vs-1"));

    monitor.onHeartbeat("vs-1", 300);

    assertFalse(monitor.isDown("vs-1"));
    assertFalse(monitor.isShutDown("vs-1"));
  }

  @Test
  void aVideoServerSilentForOverAMinute_isDownAtTheNextCheck_butNotShutDown() {
    monitor.onHeartbeat("vs-1", 0);
    elapse(Duration.ofSeconds(61));
    assertFalse(monitor.isDown("vs-1"));

    runHeartbeatCheck();

    assertTrue(monitor.isDown("vs-1"));
    assertFalse(monitor.isShutDown("vs-1"));
  }

  @Test
  void theVideoServer_isDownAndShutDown_withoutNamingIt() {
    monitor.onHeartbeat("vs-1", 100);
    assertFalse(monitor.isDown());
    assertFalse(monitor.isShutDown());

    monitor.onShutdown("vs-1", 200);
    assertTrue(monitor.isDown());
    assertTrue(monitor.isShutDown());
  }

  @Test
  void brokerDown_forgetsWhichVideoServersAreDown() {
    monitor.onShutdown("vs-1", 200);

    monitor.onBrokerLost();

    assertFalse(monitor.isDown("vs-1"));
    assertFalse(monitor.isDown());
  }
}
