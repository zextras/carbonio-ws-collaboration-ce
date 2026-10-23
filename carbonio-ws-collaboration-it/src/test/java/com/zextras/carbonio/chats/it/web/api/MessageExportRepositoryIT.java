// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@code MessageRepository#getByRoomIdAfterCursor}.
 *
 * <p>Verifies ascending keyset pagination used for bulk chat export:
 *
 * <ul>
 *   <li>Messages before or equal to {@code clearedAt} are excluded.
 *   <li>Pages are ordered ascending by (createdAt, id).
 *   <li>Two messages sharing the exact same createdAt millisecond are split across a page boundary
 *       without any message being skipped or duplicated.
 *   <li>Concatenating all pages yields every post-clearedAt message exactly once.
 * </ul>
 */
@ApiIntegrationTest
class MessageExportRepositoryIT {

  private final MessageRepository messageRepository;
  private final RoomRepository roomRepository;
  private final AppClock clock;

  public MessageExportRepositoryIT(
      MessageRepository messageRepository, RoomRepository roomRepository, Clock clock) {
    this.messageRepository = messageRepository;
    this.roomRepository = roomRepository;
    this.clock = (AppClock) clock;
  }

  // Fixed instants used across all tests
  private static final OffsetDateTime CLEARED_AT =
      OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

  // Timestamps: t0 is BEFORE clearedAt (should be excluded), t1–t5 are AFTER.
  // t2 and t3 share the EXACT same millisecond to exercise the same-timestamp edge case.
  private static final OffsetDateTime T0 = CLEARED_AT.minusSeconds(1); // excluded
  private static final OffsetDateTime T1 = CLEARED_AT.plusSeconds(1);
  private static final OffsetDateTime T2 = CLEARED_AT.plusSeconds(2); // same-ms pair start
  private static final OffsetDateTime T3 = T2; // same-ms as T2
  private static final OffsetDateTime T4 = CLEARED_AT.plusSeconds(3);
  private static final OffsetDateTime T5 = CLEARED_AT.plusSeconds(4);

  private String roomId;
  private String senderId;

  // Message IDs are chosen so that lexicographic order among same-timestamp messages is
  // deterministic and predictable.
  private String msgId0; // before clearedAt → excluded
  private String msgId1;
  private String msgId2; // same timestamp as msgId3, lexically smaller
  private String msgId3; // same timestamp as msgId2, lexically larger
  private String msgId4;
  private String msgId5;

  @BeforeEach
  void setUp() {
    roomId = UUID.randomUUID().toString();
    senderId = UUID.randomUUID().toString();

    // Ensure ids at same timestamp sort the way we expect: msgId2 < msgId3 lexically
    msgId0 = "msg-00-before-cleared";
    msgId1 = "msg-01-after";
    msgId2 = "msg-02-same-ts-a"; // lexically smaller
    msgId3 = "msg-03-same-ts-b"; // lexically larger
    msgId4 = "msg-04-later";
    msgId5 = "msg-05-latest";

    // Create the room with the sender as member
    Room room = Room.create();
    room.id(roomId)
        .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
        .name("export-test-room")
        .type(RoomTypeDto.GROUP)
        .subscriptions(
            List.of(
                Subscription.create()
                    .id(new SubscriptionId(roomId, senderId))
                    .userId(senderId)
                    .room(room)
                    .joinedAt(OffsetDateTime.now(ZoneOffset.UTC))));
    roomRepository.insert(room);

    // Insert messages with deterministic timestamps. Message.createdAt is @WhenCreated, which
    // Ebean overwrites on insert with the configured clock's current time, so the timestamp
    // cannot be set on the entity directly. Instead we drive the (test) AppClock that Ebean uses
    // (see CoreModule#getDatabase -> DatabaseConfig.setClock) and let @WhenCreated stamp the value
    // we want. T2 and T3 deliberately resolve to the same instant to exercise the same-ms edge.
    insertMessage(msgId0, T0, "before cleared");
    insertMessage(msgId1, T1, "first after cleared");
    insertMessage(msgId2, T2, "same-ms message A");
    insertMessage(msgId3, T3, "same-ms message B");
    insertMessage(msgId4, T4, "fourth after cleared");
    insertMessage(msgId5, T5, "fifth after cleared");

    clock.removeFixTime();
  }

  private void insertMessage(String id, OffsetDateTime createdAt, String text) {
    clock.fixTimeAt(createdAt.toInstant());
    messageRepository.insert(Message.create().id(id).roomId(roomId).senderId(senderId).text(text));
  }

  @Test
  @DisplayName("First page excludes messages at or before clearedAt and returns oldest-first")
  void firstPage_excludesClearedAtAndReturnsAscending() {
    List<Message> page =
        messageRepository.getByRoomIdAfterCursor(roomId, CLEARED_AT, null, null, 2);

    assertEquals(2, page.size());
    assertEquals(msgId1, page.get(0).getId());
    assertEquals(msgId2, page.get(1).getId());

    // All returned messages must have createdAt strictly after clearedAt
    assertTrue(
        page.stream().allMatch(m -> m.getCreatedAt().isAfter(CLEARED_AT)),
        "All messages in page 1 must be strictly after clearedAt");
  }

  @Test
  @DisplayName("Second page correctly continues after cursor, splitting same-ms messages")
  void secondPage_continuesAfterCursor_sameMsMessagesSplitCorrectly() {
    // Page 1 returns msgId1 and msgId2
    List<Message> page1 =
        messageRepository.getByRoomIdAfterCursor(roomId, CLEARED_AT, null, null, 2);
    assertEquals(2, page1.size());

    Message lastOfPage1 = page1.get(page1.size() - 1);

    // Page 2: cursor at (T2, msgId2) → must return msgId3, msgId4
    List<Message> page2 =
        messageRepository.getByRoomIdAfterCursor(
            roomId, CLEARED_AT, lastOfPage1.getCreatedAt(), lastOfPage1.getId(), 2);

    assertEquals(2, page2.size());
    assertEquals(
        msgId3,
        page2.get(0).getId(),
        "msgId3 shares the same timestamp as msgId2 but has a larger id — must appear on page 2");
    assertEquals(msgId4, page2.get(1).getId());
  }

  @Test
  @DisplayName("Full pagination yields every post-clearedAt message exactly once, no gaps or dupes")
  void fullPagination_yieldsAllPostClearedAtMessagesExactlyOnce() {
    int pageSize = 2;
    List<String> allIds = Stream.of(msgId1, msgId2, msgId3, msgId4, msgId5).toList();

    // Paginate through all pages
    java.util.List<String> collected = new java.util.ArrayList<>();
    OffsetDateTime cursorCreatedAt = null;
    String cursorId = null;

    while (true) {
      List<Message> page =
          messageRepository.getByRoomIdAfterCursor(
              roomId, CLEARED_AT, cursorCreatedAt, cursorId, pageSize);

      if (page.isEmpty()) {
        break;
      }

      page.forEach(m -> collected.add(m.getId()));

      Message last = page.get(page.size() - 1);
      cursorCreatedAt = last.getCreatedAt();
      cursorId = last.getId();
    }

    assertEquals(
        allIds,
        collected,
        "Concatenated pages must equal all post-clearedAt messages in ascending order, "
            + "with no duplicates and no gaps");
  }

  @Test
  @DisplayName("No lowerBound returns ALL messages in the room ascending")
  void noLowerBound_returnsAllMessagesAscending() {
    List<Message> page = messageRepository.getByRoomIdAfterCursor(roomId, null, null, null, 100);

    // All 6 messages including the one before clearedAt
    assertEquals(6, page.size());
    assertEquals(msgId0, page.get(0).getId());
    assertEquals(msgId5, page.get(page.size() - 1).getId());
  }
}
