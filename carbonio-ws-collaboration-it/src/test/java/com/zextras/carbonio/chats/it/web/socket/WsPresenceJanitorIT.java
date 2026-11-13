// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceJanitorService;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ConsulMockServer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class WsPresenceJanitorIT {

  private final WsPresenceJanitorService janitor;
  private final WsPresenceRepository wsPresenceRepository;
  private final ConsulMockServer consulMockServer;

  public WsPresenceJanitorIT(
      WsPresenceJanitorService janitor,
      WsPresenceRepository wsPresenceRepository,
      ConsulMockServer consulMockServer) {
    this.janitor = janitor;
    this.wsPresenceRepository = wsPresenceRepository;
    this.consulMockServer = consulMockServer;
  }

  @BeforeAll
  static void setUpConsul(ConsulMockServer consulMockServer) {
    consulMockServer.mockServiceIdResponse(UUID.randomUUID());
  }

  @Test
  @DisplayName(
      "Sessions of a node that is not healthy in Consul are cleaned, healthy ones are kept")
  void sweep_unhealthyNode_cleansOnlyItsSessions() {
    UUID healthyNode = UUID.randomUUID();
    UUID unhealthyNode = UUID.randomUUID();
    String healthyUser = UUID.randomUUID().toString();
    String unhealthyUser = UUID.randomUUID().toString();
    wsPresenceRepository.insertAndCountOnline(
        UUID.randomUUID().toString(), healthyUser, healthyNode.toString());
    wsPresenceRepository.insertAndCountOnline(
        UUID.randomUUID().toString(), unhealthyUser, unhealthyNode.toString());
    consulMockServer.mockHealthyServiceInstances(healthyNode);

    janitor.sweep();

    assertEquals(1, wsPresenceRepository.countOnlineByUserId(healthyUser));
    assertEquals(0, wsPresenceRepository.countOnlineByUserId(unhealthyUser));
  }
}
