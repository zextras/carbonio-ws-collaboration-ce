// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
  }

  @Nested
  @DisplayName("Single reaction per user per message")
  class SingleReaction {

    @Test
    @DisplayName("A different emoji replaces the previous one; a single reaction remains")
    void reactionReplacesPrevious() {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");

      Optional<String> firstReplaced =
          chatMessageService.addReaction(roomId.toString(), messageId, userId.toString(), "👍");
      assertTrue(firstReplaced.isEmpty());

      Optional<String> secondReplaced =
          chatMessageService.addReaction(roomId.toString(), messageId, userId.toString(), "❤️");
      assertEquals(Optional.of("👍"), secondReplaced);

      List<MessageReaction> reactions = chatMessageService.getReactions(messageId);
      assertEquals(1, reactions.size());
      assertEquals("❤️", reactions.get(0).getReaction());
    }

    @Test
    @DisplayName("Re-sending the same emoji is a no-op")
    void sameEmojiNoOp() {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");

      chatMessageService.addReaction(roomId.toString(), messageId, userId.toString(), "👍");
      Optional<String> replaced =
          chatMessageService.addReaction(roomId.toString(), messageId, userId.toString(), "👍");

      assertTrue(replaced.isEmpty());
      assertEquals(1, chatMessageService.getReactions(messageId).size());
    }

    @Test
    @DisplayName("Removing clears the user's reaction and reports the removed emoji")
    void removeClears() {
      UUID roomId = seedRoom();
      String messageId = seedMessage(roomId, "hello");

      chatMessageService.addReaction(roomId.toString(), messageId, userId.toString(), "👍");
      Optional<String> removed = chatMessageService.removeReaction(messageId, userId.toString());

      assertEquals(Optional.of("👍"), removed);
      assertTrue(chatMessageService.getReactions(messageId).isEmpty());
    }
  }
}
