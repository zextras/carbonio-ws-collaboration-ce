// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class DatabaseConnectionIT {

  private final Database database;
  private final RoomRepository roomRepository;
  private final IntegrationTestUtils integrationTestUtils;

  DatabaseConnectionIT(
      Database database, RoomRepository roomRepository, IntegrationTestUtils integrationTestUtils) {
    this.database = database;
    this.roomRepository = roomRepository;
    this.integrationTestUtils = integrationTestUtils;
  }

  @Test
  void connectionsAreNamedForPgStatActivity() {
    assertEquals(
        "ws-collaboration",
        database
            .sqlQuery("select current_setting('application_name') as name")
            .findOne()
            .getString("name"));
  }

  @Test
  void ebeanDataTimeZoneIsUtcWhateverTheJvmZone() {
    assertEquals("UTC", database.pluginApi().config().getDataTimeZone());
  }

  @Test
  void entityTimesAreReadAtUtcOffset() {
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, null, List.of(UUID.randomUUID()));
    Room room = roomRepository.getById(roomId.toString()).orElseThrow();
    assertEquals(ZoneOffset.UTC, room.getCreatedAt().getOffset());
  }
}
