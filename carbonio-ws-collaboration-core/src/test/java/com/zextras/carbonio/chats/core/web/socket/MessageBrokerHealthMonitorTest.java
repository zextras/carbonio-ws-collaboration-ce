// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MessageBrokerHealthMonitorTest {

  private EventWebSocketSessions sessions;
  private ObjectMapper objectMapper;
  private MessageBrokerHealthMonitor monitor;

  @BeforeEach
  void setUp() {
    sessions = mock(EventWebSocketSessions.class);
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    monitor = new MessageBrokerHealthMonitor(sessions, objectMapper);
  }

  @Test
  void notifyBrokerDown_broadcastsDisconnectedEvent_withCurrentSentDate() throws Exception {
    OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

    monitor.notifyBrokerDown();

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(sessions).broadcast(json.capture());
    JsonNode node = objectMapper.readTree(json.getValue());
    assertTrue(node.get("type").asText().equals(EventType.MESSAGE_BROKER_DISCONNECTED.toString()));
    OffsetDateTime sentDate = OffsetDateTime.parse(node.get("sentDate").asText());
    assertTrue(sentDate.isAfter(before));
  }

  @Test
  void notifyBrokerDown_resetsVideoServerReadiness() {
    monitor.notifyVideoServerReady();
    when(sessions.hasActiveSessionForUser(anyString())).thenReturn(true);
    assertTrue(monitor.isReadyForUser("u"));

    monitor.notifyBrokerDown();

    assertFalse(monitor.isReadyForUser("u"));
  }

  @Test
  void notifyBrokerDown_isIdempotent_broadcastsOnlyOnce() {
    monitor.notifyBrokerDown();
    monitor.notifyBrokerDown();
    monitor.notifyBrokerDown();

    verify(sessions, times(1)).broadcast(anyString());
  }

  @Test
  void notifyBrokerRecovered_whenVideoServerReady_broadcastsRestored() throws Exception {
    monitor.notifyBrokerDown();
    monitor.notifyVideoServerReady();
    clearInvocations(sessions);

    monitor.notifyBrokerRecovered();

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(sessions).broadcast(json.capture());
    JsonNode node = objectMapper.readTree(json.getValue());
    assertTrue(node.get("type").asText().equals(EventType.MESSAGE_BROKER_RESTORED.toString()));
  }

  @Test
  void notifyBrokerRecovered_whenVideoServerNotReady_defersUntilHeartbeat() {
    monitor.notifyBrokerDown();
    clearInvocations(sessions);

    monitor.notifyBrokerRecovered();
    verify(sessions, never()).broadcast(anyString());

    monitor.notifyVideoServerReady();
    verify(sessions).broadcast(anyString());
  }

  @Test
  void notifyVideoServerReady_atStartup_doesNotBroadcast() {
    monitor.notifyVideoServerReady();

    verify(sessions, never()).broadcast(anyString());
  }

  @Test
  void notifyVideoServerReady_calledTwiceAfterDeferredRestore_broadcastsOnlyOnce() {
    monitor.notifyBrokerDown();
    clearInvocations(sessions);
    monitor.notifyBrokerRecovered();
    monitor.notifyVideoServerReady();
    monitor.notifyVideoServerReady();

    verify(sessions, times(1)).broadcast(anyString());
  }

  @Test
  void isReadyForUser_requiresBrokerUpAndVideoServerReadyAndActiveSession() {
    when(sessions.hasActiveSessionForUser("u")).thenReturn(true);
    assertFalse(monitor.isReadyForUser("u")); // videoServerReady=false

    monitor.notifyVideoServerReady();
    assertTrue(monitor.isReadyForUser("u"));

    when(sessions.hasActiveSessionForUser("u")).thenReturn(false);
    assertFalse(monitor.isReadyForUser("u"));

    when(sessions.hasActiveSessionForUser("u")).thenReturn(true);
    monitor.notifyBrokerDown();
    assertFalse(monitor.isReadyForUser("u"));
  }
}
