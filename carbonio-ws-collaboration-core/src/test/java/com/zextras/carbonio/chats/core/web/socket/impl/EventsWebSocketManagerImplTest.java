// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.impl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
import com.zextras.carbonio.chats.core.web.socket.action.WsActionRouter;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class EventsWebSocketManagerImplTest {

  private EventsWebSocketManagerImpl manager;
  private ParticipantService participantService;
  private Session session;
  private ObjectMapper objectMapper;

  private static final UUID SENDER_USER_ID =
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID MEETING_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID OTHER_USER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

  @BeforeEach
  void setUp() {
    RabbitConnectionPoolService rabbitPool = mock(RabbitConnectionPoolService.class);
    objectMapper = new ObjectMapper();
    WebsocketVersionMigrator migrator = mock(WebsocketVersionMigrator.class);
    CacheVideoServerSession cacheVideoServerSession = mock(CacheVideoServerSession.class);
    participantService = mock(ParticipantService.class);
    EventWebSocketSessions sessions = mock(EventWebSocketSessions.class);

    manager =
        new EventsWebSocketManagerImpl(
            rabbitPool,
            objectMapper,
            migrator,
            cacheVideoServerSession,
            participantService,
            mock(WsActionRouter.class),
            mock(WsPresenceManager.class),
            sessions);

    session = mock(Session.class);
    HttpSession httpSession = mock(HttpSession.class);
    Map<String, Object> userProps = new HashMap<>();
    userProps.put(HttpSession.class.getName(), httpSession);
    when(session.getUserProperties()).thenReturn(userProps);
    when(httpSession.getAttribute("userId")).thenReturn(SENDER_USER_ID.toString());
    when(session.isOpen()).thenReturn(true);
    when(session.getId()).thenReturn(UUID.randomUUID().toString());
  }

  @Test
  @DisplayName(
      "UplinkStatusUpdate broadcast: delegates to participantService with no target, all optionals"
          + " null")
  void uplinkStatusUpdate_broadcast_delegatesWithoutTarget() {
    long changedAt = 1700000000000L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName("UplinkStatusUpdate with networkScore present: networkScore is relayed")
  void uplinkStatusUpdate_networkScorePresent_relayed() {
    long changedAt = 1700000000000L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .put("networkScore", 7.5)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            eq(java.math.BigDecimal.valueOf(7.5)),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName("UplinkStatusUpdate with networkScore absent: relays null networkScore")
  void uplinkStatusUpdate_networkScoreAbsent_relaysNull() {
    long changedAt = 1700000000002L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName("UplinkStatusUpdate with valid maxUplinkTier in payload: maxUplinkTier is relayed")
  void uplinkStatusUpdate_maxUplinkTierPresent_relayed() {
    long changedAt = 1700000000000L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .put("networkScore", 7.5)
            .put("maxUplinkTier", 2)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            eq(java.math.BigDecimal.valueOf(7.5)),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            eq(2),
            isNull());
  }

  @Test
  @DisplayName("UplinkStatusUpdate with maxUplinkTier absent: relays null maxUplinkTier")
  void uplinkStatusUpdate_maxUplinkTierAbsent_relaysNull() {
    long changedAt = 1700000000002L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName(
      "UplinkStatusUpdate with out-of-range maxUplinkTier: relays null, message still delivered")
  void uplinkStatusUpdate_maxUplinkTierOutOfRange_relaysNull() {
    long changedAt = 1700000000004L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .put("maxUplinkTier", 5)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName(
      "UplinkStatusUpdate with valid maxHardwareTier in payload: maxHardwareTier is relayed")
  void uplinkStatusUpdate_maxHardwareTierPresent_relayed() {
    long changedAt = 1700000000005L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .put("networkScore", 5.0)
            .put("maxUplinkTier", 1)
            .put("maxHardwareTier", 2)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            eq(java.math.BigDecimal.valueOf(5.0)),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            eq(1),
            eq(2));
  }

  @Test
  @DisplayName("UplinkStatusUpdate with maxHardwareTier absent: relays null maxHardwareTier")
  void uplinkStatusUpdate_maxHardwareTierAbsent_relaysNull() {
    long changedAt = 1700000000006L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName(
      "UplinkStatusUpdate with out-of-range maxHardwareTier: relays null, message still delivered")
  void uplinkStatusUpdate_maxHardwareTierOutOfRange_relaysNull() {
    long changedAt = 1700000000007L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .put("maxHardwareTier", 99)
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            isNull(),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName("UplinkStatusUpdate unicast: delegates with the 'to' target user")
  void uplinkStatusUpdate_unicast_delegatesWithTarget() {
    long changedAt = 1700000000001L;
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .put("changedAt", changedAt)
            .put("to", OTHER_USER_ID.toString())
            .toString();

    manager.onMessage(session, json);

    verify(participantService)
        .relayUplinkStatus(
            eq(MEETING_ID.toString()),
            isNull(),
            eq(changedAt),
            eq(OTHER_USER_ID.toString()),
            eq(SENDER_USER_ID.toString()),
            isNull(),
            isNull());
  }

  @Test
  @DisplayName("UplinkStatusUpdate: missing meetingId is silently dropped, no delegation")
  void uplinkStatusUpdate_missingMeetingId_dropped() {
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("changedAt", 1700000000003L)
            .toString();

    manager.onMessage(session, json);

    verifyNoInteractions(participantService);
  }

  @Test
  @DisplayName("UplinkStatusUpdate: missing changedAt is silently dropped, no delegation")
  void uplinkStatusUpdate_missingChangedAt_dropped() {
    String json =
        objectMapper
            .createObjectNode()
            .put("type", "UplinkStatusUpdate")
            .put("meetingId", MEETING_ID.toString())
            .toString();

    manager.onMessage(session, json);

    verifyNoInteractions(participantService);
  }
}
