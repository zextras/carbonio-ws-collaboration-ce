// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository.Claim;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import io.ebean.Database;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class MessageSendDedupRepositoryIT {

  private static final String ROOM = "room-1";
  private static final String SENDER = "sender-1";

  private final MessageSendDedupRepository repository;
  private final Database database;

  public MessageSendDedupRepositoryIT(MessageSendDedupRepository repository, Database database) {
    this.repository = repository;
    this.database = database;
  }

  private long count(String sql) {
    return database.sqlQuery(sql).findOne().getLong("n");
  }

  private String messageIdOf(String roomId, String senderId, String tempId) {
    return database
        .sqlQuery(
            "SELECT message_id FROM chats.message_send_dedup"
                + " WHERE room_id = :roomId AND sender_id = :senderId AND temp_id = :tempId")
        .setParameter("roomId", roomId)
        .setParameter("senderId", senderId)
        .setParameter("tempId", tempId)
        .findOneOrEmpty()
        .map(row -> row.getString("message_id"))
        .orElse(null);
  }

  private void insertClaim(String tempId, String messageId, String age) {
    database
        .sqlUpdate(
            "INSERT INTO chats.message_send_dedup"
                + " (room_id, sender_id, temp_id, message_id, created_at)"
                + " VALUES (:roomId, :senderId, :tempId, :messageId,"
                + " CAST(clock_timestamp() AS timestamp) - CAST(:age AS interval))")
        .setParameter("roomId", ROOM)
        .setParameter("senderId", SENDER)
        .setParameter("tempId", tempId)
        .setParameter("messageId", messageId)
        .setParameter("age", age)
        .execute();
  }

  static <T> List<T> concurrently(int threads, Callable<T> task) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<T>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return task.call();
                }));
      }
      start.countDown();
      List<T> results = new ArrayList<>();
      for (Future<T> future : futures) {
        results.add(future.get(30, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }

  @Nested
  @DisplayName("Claim tests")
  class ClaimTests {

    @Test
    @DisplayName("The first claim wins and a second one gets the first message id")
    void claim_firstWins() {
      assertTrue(repository.claim(ROOM, SENDER, "t-1", "m-1").claimed());

      Claim second = repository.claim(ROOM, SENDER, "t-1", "m-2");

      assertFalse(second.claimed());
      assertEquals("m-1", second.existingMessageId());
      assertEquals("m-1", messageIdOf(ROOM, SENDER, "t-1"));
    }

    @Test
    @DisplayName("A claim older than one hour is taken over")
    void claim_expiredIsTakenOver() {
      insertClaim("t-1", "m-old", "61 minutes");

      Claim claim = repository.claim(ROOM, SENDER, "t-1", "m-new");

      assertTrue(claim.claimed());
      assertEquals("m-new", messageIdOf(ROOM, SENDER, "t-1"));
    }

    @Test
    @DisplayName("A claim younger than one hour is still live")
    void claim_liveIsKept() {
      insertClaim("t-1", "m-old", "59 minutes");

      Claim claim = repository.claim(ROOM, SENDER, "t-1", "m-new");

      assertFalse(claim.claimed());
      assertEquals("m-old", claim.existingMessageId());
    }

    @Test
    @DisplayName("The key is room, sender and tempId")
    void claim_keyIsRoomSenderTempId() {
      assertTrue(repository.claim(ROOM, SENDER, "t-1", "m-1").claimed());
      assertTrue(repository.claim("room-2", SENDER, "t-1", "m-2").claimed());
      assertTrue(repository.claim(ROOM, "sender-2", "t-1", "m-3").claimed());
      assertEquals(3, count("SELECT count(*) AS n FROM chats.message_send_dedup"));
    }

    @Test
    @DisplayName("Concurrent claims: exactly one wins, every loser gets the winner's message id")
    void claim_concurrent() throws Exception {
      record Attempt(String messageId, Claim claim) {}

      List<Attempt> attempts =
          concurrently(
              8,
              () -> {
                String messageId = UUID.randomUUID().toString();
                return new Attempt(messageId, repository.claim(ROOM, SENDER, "race", messageId));
              });

      List<Attempt> winners = attempts.stream().filter(a -> a.claim().claimed()).toList();
      assertEquals(1, winners.size());
      String winner = winners.get(0).messageId();
      attempts.stream()
          .filter(a -> !a.claim().claimed())
          .forEach(a -> assertEquals(winner, a.claim().existingMessageId()));
      assertEquals(winner, messageIdOf(ROOM, SENDER, "race"));
    }
  }

  @Nested
  @DisplayName("Release tests")
  class ReleaseTests {

    @Test
    @DisplayName("Releasing its own claim frees the tempId")
    void release_ownClaim() {
      repository.claim(ROOM, SENDER, "t-1", "m-1");

      repository.release(ROOM, SENDER, "t-1", "m-1");

      assertNull(messageIdOf(ROOM, SENDER, "t-1"));
      assertTrue(repository.claim(ROOM, SENDER, "t-1", "m-2").claimed());
    }

    @Test
    @DisplayName("Releasing with another message id leaves the claim in place")
    void release_otherMessageId() {
      repository.claim(ROOM, SENDER, "t-1", "m-1");

      repository.release(ROOM, SENDER, "t-1", "m-2");

      assertEquals("m-1", messageIdOf(ROOM, SENDER, "t-1"));
    }
  }

  @Nested
  @DisplayName("Purge tests")
  class PurgeTests {

    @Test
    @DisplayName("Only claims older than one hour are deleted, in batches")
    void purgeExpired_batches() {
      insertClaim("old-1", "m-1", "61 minutes");
      insertClaim("old-2", "m-2", "2 hours");
      insertClaim("old-3", "m-3", "3 days");
      insertClaim("fresh", "m-4", "10 minutes");

      assertEquals(2, repository.purgeExpired(2));
      assertEquals(1, repository.purgeExpired(2));
      assertEquals(0, repository.purgeExpired(2));

      assertEquals(1, count("SELECT count(*) AS n FROM chats.message_send_dedup"));
      assertEquals("m-4", messageIdOf(ROOM, SENDER, "fresh"));
    }
  }
}
