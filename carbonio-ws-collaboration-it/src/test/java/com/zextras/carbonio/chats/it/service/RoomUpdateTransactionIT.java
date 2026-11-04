// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
@DisplayName("A room update and its system lines are written in one transaction")
class RoomUpdateTransactionIT {

  private final RoomService roomService;
  private final IntegrationTestUtils integrationTestUtils;
  private final Database database;

  public RoomUpdateTransactionIT(
      RoomService roomService, IntegrationTestUtils integrationTestUtils, Database database) {
    this.roomService = roomService;
    this.integrationTestUtils = integrationTestUtils;
    this.database = database;
  }

  private UUID seedRoom(UUID ownerId) {
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "initial", List.of(ownerId, UUID.randomUUID()));
    return roomId;
  }

  private String storedName(UUID roomId) {
    return database
        .sqlQuery("SELECT name FROM chats.room WHERE id = :id")
        .setParameter("id", roomId.toString())
        .findOne()
        .getString("name");
  }

  private String latestNameLine(UUID roomId) {
    return database
        .sqlQuery(
            "SELECT content FROM chats.system_event WHERE room_id = :id AND type ="
                + " 'ROOM_NAME_CHANGED' ORDER BY created_at DESC, id DESC LIMIT 1")
        .setParameter("id", roomId.toString())
        .findOne()
        .getString("content");
  }

  @Test
  @DisplayName("A failing line insert rolls the rename back")
  void failingLineRollsBackTheRename() {
    UUID ownerId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    UUID roomId = seedRoom(ownerId);
    String trigger = "it_fail_line_" + roomId.toString().replace("-", "");
    database
        .sqlUpdate(
            "CREATE FUNCTION chats."
                + trigger
                + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.room_id = '"
                + roomId
                + "' THEN RAISE EXCEPTION 'line refused'; END IF; RETURN NEW; END $$")
        .execute();
    database
        .sqlUpdate(
            "CREATE TRIGGER "
                + trigger
                + " BEFORE INSERT ON chats.system_event FOR EACH ROW EXECUTE FUNCTION chats."
                + trigger
                + "()")
        .execute();
    try {
      assertThrows(
          RuntimeException.class,
          () ->
              roomService.updateRoom(
                  roomId,
                  RoomEditableFieldsDto.create().name("renamed"),
                  UserPrincipal.create(ownerId)));

      assertEquals("initial", storedName(roomId));
    } finally {
      database.sqlUpdate("DROP TRIGGER " + trigger + " ON chats.system_event").execute();
      database.sqlUpdate("DROP FUNCTION chats." + trigger + "()").execute();
    }
  }

  @Test
  @DisplayName("After concurrent renames the latest name line names the stored name")
  void concurrentRenamesKeepLineOrder() throws Exception {
    UUID ownerId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    UserPrincipal owner = UserPrincipal.create(ownerId);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      for (int round = 0; round < 20; round++) {
        UUID roomId = seedRoom(ownerId);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> renames =
            List.of("A" + round, "B" + round).stream()
                .<Future<?>>map(
                    name ->
                        pool.submit(
                            () -> {
                              start.await();
                              return roomService.updateRoom(
                                  roomId, RoomEditableFieldsDto.create().name(name), owner);
                            }))
                .toList();
        start.countDown();
        for (Future<?> rename : renames) {
          rename.get(30, TimeUnit.SECONDS);
        }

        String stored = storedName(roomId);
        assertTrue(
            latestNameLine(roomId).contains("\"" + stored + "\""),
            "round " + round + ": stored " + stored + ", latest line " + latestNameLine(roomId));
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
