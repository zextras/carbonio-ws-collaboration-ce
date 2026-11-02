// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import io.ebean.Transaction;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
@DisplayName("A pin that waits for a concurrent pin takes its time after the wait")
class PinTimeIT {

  private final MessagePinRepository pinRepository;
  private final IntegrationTestUtils integrationTestUtils;
  private final Database database;

  public PinTimeIT(
      MessagePinRepository pinRepository,
      IntegrationTestUtils integrationTestUtils,
      Database database) {
    this.pinRepository = pinRepository;
    this.integrationTestUtils = integrationTestUtils;
    this.database = database;
  }

  private boolean pinStatementIsWaitingForALock() {
    try (Transaction observer = database.createTransaction()) {
      return database
              .sqlQuery(
                  "SELECT count(*) AS waiting FROM pg_stat_activity"
                      + " WHERE wait_event_type = 'Lock' AND query LIKE '%chats.message_pin%'")
              .usingTransaction(observer)
              .findOne()
              .getLong("waiting")
          > 0;
    }
  }

  private OffsetDateTime pinnedAt(UUID roomId) {
    try (Transaction observer = database.createTransaction()) {
      return database
          .sqlQuery("SELECT pinned_at FROM chats.message_pin WHERE room_id = :roomId")
          .setParameter("roomId", roomId.toString())
          .usingTransaction(observer)
          .findOne()
          .getTimestamp("pinned_at")
          .toInstant()
          .atOffset(ZoneOffset.UTC);
    }
  }

  @Test
  @DisplayName("The surviving pin has the later time")
  void survivingPinHasTheLaterTime() throws Exception {
    UUID userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(userId));
    String messageA = UUID.randomUUID().toString();
    String messageB = UUID.randomUUID().toString();
    integrationTestUtils.generateAndSaveMessage(messageA, roomId, userId, "A");
    integrationTestUtils.generateAndSaveMessage(messageB, roomId, userId, "B");

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Transaction transactionA = database.beginTransaction()) {
      Optional<MessagePin> pinA = pinRepository.pin(roomId.toString(), messageA, userId.toString());
      assertTrue(pinA.isPresent());

      Future<Optional<MessagePin>> pinB =
          executor.submit(() -> pinRepository.pin(roomId.toString(), messageB, userId.toString()));
      assertTimeoutPreemptively(
          Duration.ofSeconds(30),
          () -> {
            while (!pinStatementIsWaitingForALock()) {
              Thread.onSpinWait();
            }
          });
      OffsetDateTime beforeRelease =
          database
              .sqlQuery("SELECT clock_timestamp() AS now")
              .usingTransaction(transactionA)
              .findOne()
              .getTimestamp("now")
              .toInstant()
              .atOffset(ZoneOffset.UTC);
      assertTrue(pinA.get().getPinnedAt().isBefore(beforeRelease));

      transactionA.commit();
      Optional<MessagePin> resultB = pinB.get(30, TimeUnit.SECONDS);

      assertTrue(resultB.isPresent());
      assertEquals(messageB, resultB.get().getMessageId());
      assertTrue(resultB.get().getPinnedAt().isAfter(beforeRelease));
      OffsetDateTime stored = pinnedAt(roomId);
      assertEquals(resultB.get().getPinnedAt().toInstant(), stored.toInstant());
    } finally {
      executor.shutdownNow();
    }
  }
}
