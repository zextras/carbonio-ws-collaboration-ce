// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.Removed;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetKind;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetResult;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies, against a real PostgreSQL instance with the actual Flyway schema, that pins are single
 * per room and reactions are single per user per message (the MongooseIM parity behaviour).
 */
@ApiIntegrationTest
class PinReactionSingleIT {

  private final ChatMessageService chatMessageService;
  private final IntegrationTestUtils integrationTestUtils;

  public PinReactionSingleIT(
      ChatMessageService chatMessageService, IntegrationTestUtils integrationTestUtils) {
    this.chatMessageService = chatMessageService;
    this.integrationTestUtils = integrationTestUtils;
  }

  private static UUID userId;

  @BeforeAll
  static void initAll() {
    userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  }

  private UUID seedRoom() {
    UUID roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(userId));
    return roomId;
  }

  private String seedMessage(UUID roomId, String text) {
    String messageId = UUID.randomUUID().toString();
    integrationTestUtils.generateAndSaveMessage(messageId, roomId, userId, text);
    return messageId;
  }

  @Nested
  @DisplayName("Single pin per room")
  class SinglePin {

    @Test
    @DisplayName("Pinning a second message replaces the first; the room keeps a single pin")
    void pinReplacesPrevious() {
      UUID roomId = seedRoom();
      String messageA = seedMessage(roomId, "A");
      String messageB = seedMessage(roomId, "B");

      chatMessageService.pinMessage(roomId.toString(), messageA, userId.toString());
      chatMessageService.pinMessage(roomId.toString(), messageB, userId.toString());

      List<MessagePin> pins = chatMessageService.getPinnedMessages(roomId.toString());
      assertEquals(1, pins.size());
      assertEquals(messageB, pins.get(0).getMessageId());
    }

    @Test
    @DisplayName("Unpinning removes the room pin")
    void unpinRemoves() {
      UUID roomId = seedRoom();
      String messageA = seedMessage(roomId, "A");

      chatMessageService.pinMessage(roomId.toString(), messageA, userId.toString());
      chatMessageService.unpinMessage(roomId.toString(), messageA, userId.toString());

      assertTrue(chatMessageService.getPinnedMessages(roomId.toString()).isEmpty());
    }

    @Test
    @DisplayName("Concurrent pins in one room all succeed and leave a single pin")
    void concurrentPins() throws Exception {
      UUID roomId = seedRoom();
      List<String> messages = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        messages.add(seedMessage(roomId, "m" + i));
      }
      CountDownLatch start = new CountDownLatch(1);
      ExecutorService executor = Executors.newFixedThreadPool(messages.size());
      try {
        List<Future<PinResult>> results = new ArrayList<>();
        for (String messageId : messages) {
          results.add(
              executor.submit(
                  () -> {
                    start.await();
                    return chatMessageService.pinMessage(
                        roomId.toString(), messageId, userId.toString());
                  }));
        }
        start.countDown();
        for (int i = 0; i < results.size(); i++) {
          PinResult pinned = results.get(i).get(30, TimeUnit.SECONDS);
          assertEquals(messages.get(i), pinned.message().getId());
          assertEquals("MESSAGE_PINNED", pinned.systemEvent().getType());
        }
      } finally {
        executor.shutdownNow();
      }

      List<MessagePin> pins = chatMessageService.getPinnedMessages(roomId.toString());
      assertEquals(1, pins.size());
      assertTrue(messages.contains(pins.get(0).getMessageId()));
    }
  }

  @Nested
  @DisplayName("Single reaction per user per message")
  class SingleReaction {

    @Test
    @DisplayName("A different emoji replaces the previous one; a single reaction remains")
    void reactionReplacesPrevious() {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");

      SetResult first =
          chatMessageService.setReaction(roomId.toString(), messageId, userId.toString(), "👍");
      assertEquals(SetKind.ADDED, first.kind());
      assertNull(first.previousReaction());

      SetResult second =
          chatMessageService.setReaction(roomId.toString(), messageId, userId.toString(), "❤️");
      assertEquals(SetKind.UPDATED, second.kind());
      assertEquals("👍", second.previousReaction());
      assertTrue(second.updatedAt().isAfter(first.updatedAt()));

      List<MessageReaction> reactions = chatMessageService.getReactions(messageId);
      assertEquals(1, reactions.size());
      assertEquals("❤️", reactions.get(0).getReaction());
      assertTrue(reactions.get(0).getUpdatedAt().isEqual(second.updatedAt()));
    }

    @Test
    @DisplayName("Re-sending the same emoji is a no-op")
    void sameEmojiNoOp() {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");

      SetResult first =
          chatMessageService.setReaction(roomId.toString(), messageId, userId.toString(), "👍");
      SetResult again =
          chatMessageService.setReaction(roomId.toString(), messageId, userId.toString(), "👍");

      assertEquals(SetKind.UNCHANGED, again.kind());
      assertNull(again.updatedAt());
      List<MessageReaction> reactions = chatMessageService.getReactions(messageId);
      assertEquals(1, reactions.size());
      assertTrue(reactions.get(0).getUpdatedAt().isEqual(first.updatedAt()));
    }

    @Test
    @DisplayName("Removing clears the user's reaction and reports the removed emoji")
    void removeClears() {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");

      SetResult set =
          chatMessageService.setReaction(roomId.toString(), messageId, userId.toString(), "👍");
      Optional<Removed> removed =
          chatMessageService.removeReaction(roomId.toString(), messageId, userId.toString());

      assertEquals("👍", removed.orElseThrow().reaction());
      assertTrue(removed.get().deletedAt().isAfter(set.updatedAt()));
      assertTrue(chatMessageService.getReactions(messageId).isEmpty());
    }

    @Test
    @DisplayName("Concurrent first-time sets of the same user end with one row and one ADDED")
    void concurrentSets() throws Exception {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");
      int threads = 8;
      ExecutorService executor = Executors.newFixedThreadPool(threads);
      CountDownLatch start = new CountDownLatch(1);
      try {
        List<Future<SetResult>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
          String emoji = "e" + i;
          results.add(
              executor.submit(
                  () -> {
                    start.await();
                    return chatMessageService.setReaction(
                        roomId.toString(), messageId, userId.toString(), emoji);
                  }));
        }
        start.countDown();
        List<SetResult> done = new ArrayList<>();
        for (Future<SetResult> f : results) {
          done.add(f.get(30, TimeUnit.SECONDS));
        }
        assertEquals(1, done.stream().filter(r -> r.kind() == SetKind.ADDED).count());
      } finally {
        executor.shutdownNow();
      }
      assertEquals(1, chatMessageService.getReactions(messageId).size());
    }
  }
}
