// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class OnlineUsersSharingRoomIT {

  private final RoomRepository roomRepository;
  private final WsPresenceRepository wsPresenceRepository;
  private final IntegrationTestUtils integrationTestUtils;

  public OnlineUsersSharingRoomIT(
      RoomRepository roomRepository,
      WsPresenceRepository wsPresenceRepository,
      IntegrationTestUtils integrationTestUtils) {
    this.roomRepository = roomRepository;
    this.wsPresenceRepository = wsPresenceRepository;
    this.integrationTestUtils = integrationTestUtils;
  }

  private void online(UUID userId) {
    wsPresenceRepository.insertAndCountOnline(
        UUID.randomUUID().toString(), userId.toString(), "svc");
  }

  @Test
  @DisplayName("Returns only the online users sharing a room, once each, excluding the user")
  void getOnlineUserIdsSharingRoomWith_returnsOnlineRoommatesOnly() {
    UUID me = UUID.randomUUID();
    UUID onlineInBothRooms = UUID.randomUUID();
    UUID offlineRoommate = UUID.randomUUID();
    UUID onlineStranger = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        UUID.randomUUID(), RoomTypeDto.GROUP, "a", List.of(me, onlineInBothRooms, offlineRoommate));
    integrationTestUtils.generateAndSaveRoom(
        UUID.randomUUID(), RoomTypeDto.GROUP, "b", List.of(me, onlineInBothRooms));
    integrationTestUtils.generateAndSaveRoom(
        UUID.randomUUID(), RoomTypeDto.GROUP, "c", List.of(onlineStranger, offlineRoommate));
    online(me);
    online(onlineInBothRooms);
    online(onlineInBothRooms);
    online(onlineStranger);

    List<String> result = roomRepository.getOnlineUserIdsSharingRoomWith(me.toString());

    assertEquals(List.of(onlineInBothRooms.toString()), result);
  }

  @Test
  @DisplayName("Returns nothing when every roommate is offline")
  void getOnlineUserIdsSharingRoomWith_allRoommatesOffline_returnsEmpty() {
    UUID me = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        UUID.randomUUID(), RoomTypeDto.GROUP, "a", List.of(me, UUID.randomUUID()));
    online(me);

    assertEquals(
        Set.of(), new HashSet<>(roomRepository.getOnlineUserIdsSharingRoomWith(me.toString())));
  }
}
