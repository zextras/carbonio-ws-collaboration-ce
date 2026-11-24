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
class VideoServerHealthTest {

  private static final String RESTORED = "MessageBrokerRestored";
  private static final String DISCONNECTED = "MessageBrokerDisconnected";

  private final EventWebSocketSessions sessions = mock(EventWebSocketSessions.class);
  private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private Instant now = Instant.parse("2026-11-13T10:00:00Z");
  private VideoServerHealth health;

  @BeforeEach
  void setUp() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenAnswer(invocation -> now);
    health = new VideoServerHealth(sessions, objectMapper, clock, scheduler);
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
    assertThrows(VideoServerException.class, () -> health.checkAvailable());
    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void firstHeartbeat_allowsMeetingMedia_andBroadcastsRestored() {
    health.onHeartbeat("janus", 0);

    assertDoesNotThrow(() -> health.checkAvailable());
    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void furtherHeartbeats_broadcastNothingMore() {
    health.onHeartbeat("janus", 0);
    health.onHeartbeat("janus", 0);
    health.onHeartbeat("janus", 0);

    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void heartbeatWithinTheLastMinute_keepsMeetingMediaAllowed() {
    health.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(60));

    runHeartbeatCheck();

    assertDoesNotThrow(() -> health.checkAvailable());
    assertEquals(List.of(RESTORED), broadcastTypes());
  }

  @Test
  void noHeartbeatForOverAMinute_broadcastsDisconnected_andRefusesMeetingMedia() {
    health.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(61));

    runHeartbeatCheck();

    assertThrows(VideoServerException.class, () -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void staleHeartbeatCheckedAgain_broadcastsDisconnectedOnce() {
    health.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(61));

    runHeartbeatCheck();
    runHeartbeatCheck();

    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void heartbeatAfterItWentStale_allowsMeetingMediaAgain_andBroadcastsRestored() {
    health.onHeartbeat("janus", 0);
    elapse(Duration.ofSeconds(61));
    runHeartbeatCheck();

    health.onHeartbeat("janus", 0);
    runHeartbeatCheck();

    assertDoesNotThrow(() -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void brokerDown_broadcastsDisconnectedAtOnce_andRefusesMeetingMedia() {
    health.onHeartbeat("janus", 0);

    health.onBrokerLost();

    assertThrows(VideoServerException.class, () -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void brokerDownWhileMeetingMediaIsAlreadyRefused_broadcastsNothing() {
    health.onBrokerLost();
    health.onBrokerLost();

    assertEquals(List.of(), broadcastTypes());
  }

  @Test
  void heartbeatWhileTheBrokerConnectionIsDown_keepsMeetingMediaRefused() {
    health.onHeartbeat("janus", 0);
    health.onBrokerLost();

    health.onHeartbeat("janus", 0);

    assertThrows(VideoServerException.class, () -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void brokerRecovered_afterAHeartbeatSinceTheDrop_allowsMeetingMedia_andBroadcastsRestored() {
    health.onHeartbeat("janus", 0);
    health.onBrokerLost();
    health.onHeartbeat("janus", 0);

    health.onBrokerRecovered();

    assertDoesNotThrow(() -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void brokerRecovered_withoutAHeartbeatSinceTheDrop_waitsForTheNextHeartbeat() {
    health.onHeartbeat("janus", 0);
    health.onBrokerLost();

    health.onBrokerRecovered();

    assertThrows(VideoServerException.class, () -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());

    health.onHeartbeat("janus", 0);

    assertDoesNotThrow(() -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void broadcastEvents_carryTheClockTimeAsSentDate() {
    health.onHeartbeat("janus", 0);
    health.onBrokerLost();

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
    health.notifyIfUnavailable(session);
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
    health.onHeartbeat("janus", 0);

    RemoteEndpoint.Async remote = openedSession();

    verify(remote, never()).sendText(anyString());
  }

  @Test
  void oneVideoServerStillSendingHeartbeats_keepsMeetingMediaAllowed() {
    health.onHeartbeat("vs-1", 0);
    health.onHeartbeat("vs-2", 0);
    elapse(Duration.ofSeconds(45));
    health.onHeartbeat("vs-2", 0);
    elapse(Duration.ofSeconds(30));
    runHeartbeatCheck();

    assertDoesNotThrow(() -> health.checkAvailable());
    assertFalse(health.isAvailable("vs-1"));
    assertTrue(health.isAvailable("vs-2"));
  }

  @Test
  void aVideoServerThatNeverNamedItself_isAvailableWhileAnyHeartbeatArrives() {
    assertFalse(health.isAvailable("vs-1"));

    health.onHeartbeat("", 0);

    assertTrue(health.isAvailable("vs-1"));
  }

  @Test
  void brokerDown_makesEveryVideoServerUnavailable() {
    health.onHeartbeat("vs-1", 0);

    health.onBrokerLost();
    health.onBrokerRecovered();

    assertFalse(health.isAvailable("vs-1"));
  }

  @Test
  void shutdownOfTheOnlyVideoServer_refusesMeetingMediaAtOnce_andBroadcastsDisconnected() {
    health.onHeartbeat("vs-1", 100);

    health.onShutdown("vs-1", 200);

    assertFalse(health.isAvailable("vs-1"));
    assertThrows(VideoServerException.class, () -> health.checkAvailable());
    assertEquals(List.of(RESTORED, DISCONNECTED), broadcastTypes());
  }

  @Test
  void heartbeatAfterAShutdown_makesTheVideoServerAvailableAgain() {
    health.onHeartbeat("vs-1", 100);
    health.onShutdown("vs-1", 200);

    health.onHeartbeat("vs-1", 300);

    assertTrue(health.isAvailable("vs-1"));
    assertEquals(List.of(RESTORED, DISCONNECTED, RESTORED), broadcastTypes());
  }

  @Test
  void shutdownRelayedAfterANewerHeartbeat_isIgnored() {
    health.onHeartbeat("vs-1", 300);

    health.onShutdown("vs-1", 200);

    assertTrue(health.isAvailable("vs-1"));
  }

  @Test
  void heartbeatRelayedWithoutATimestampByAnOlderInstance_isStillApplied() {
    health.onShutdown("vs-1", 200);

    health.onHeartbeat("vs-1", 0);

    assertTrue(health.isAvailable("vs-1"));
  }

  @Test
  void aVideoServerNeverHeardFrom_isNotDown() {
    assertFalse(health.isDown("vs-1"));
    assertFalse(health.isDown());
    assertFalse(health.isShutDown("vs-1"));
    assertFalse(health.isShutDown());
  }

  @Test
  void aShutDownVideoServer_isDownAndShutDown_untilItsNextHeartbeat() {
    health.onHeartbeat("vs-1", 100);
    health.onShutdown("vs-1", 200);

    assertTrue(health.isDown("vs-1"));
    assertTrue(health.isShutDown("vs-1"));

    health.onHeartbeat("vs-1", 300);

    assertFalse(health.isDown("vs-1"));
    assertFalse(health.isShutDown("vs-1"));
  }

  @Test
  void aVideoServerSilentForOverAMinute_isDownAtTheNextCheck_butNotShutDown() {
    health.onHeartbeat("vs-1", 0);
    elapse(Duration.ofSeconds(61));
    assertFalse(health.isDown("vs-1"));

    runHeartbeatCheck();

    assertTrue(health.isDown("vs-1"));
    assertFalse(health.isShutDown("vs-1"));
  }

  @Test
  void theVideoServer_isDownAndShutDown_withoutNamingIt() {
    health.onHeartbeat("vs-1", 100);
    assertFalse(health.isDown());
    assertFalse(health.isShutDown());

    health.onShutdown("vs-1", 200);
    assertTrue(health.isDown());
    assertTrue(health.isShutDown());
  }

  @Test
  void brokerDown_forgetsWhichVideoServersAreDown() {
    health.onShutdown("vs-1", 200);

    health.onBrokerLost();

    assertFalse(health.isDown("vs-1"));
    assertFalse(health.isDown());
  }
}
