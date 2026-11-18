// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.web.api.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.model.MemberDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class SubscriptionMapperImplTest {

  private final SubscriptionMapper subscriptionMapper;

  SubscriptionMapperImplTest(SubscriptionMapper subscriptionMapper) {
    this.subscriptionMapper = subscriptionMapper;
  }

  @Test
  @DisplayName("Subscriptions map to members keeping the order, the user and the owner flag")
  void ent2memberDto_subscriptions() {
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    UUID user3Id = UUID.randomUUID();

    List<MemberDto> members =
        subscriptionMapper.ent2memberDto(
            List.of(
                Subscription.create().userId(user1Id.toString()).owner(true),
                Subscription.create().userId(user2Id.toString()).owner(false),
                Subscription.create().userId(user3Id.toString()).owner(false)));

    assertEquals(3, members.size());
    assertEquals(user1Id, members.get(0).getUserId());
    assertEquals(user2Id, members.get(1).getUserId());
    assertEquals(user3Id, members.get(2).getUserId());
    assertTrue(members.get(0).isOwner());
    assertFalse(members.get(1).isOwner());
    assertFalse(members.get(2).isOwner());
  }

  @Test
  @DisplayName("A missing list maps to an empty list and a missing subscription to null")
  void ent2memberDto_nullAndEmpty() {
    assertEquals(List.of(), subscriptionMapper.ent2memberDto((List<Subscription>) null));
    assertNull(subscriptionMapper.ent2memberDto((Subscription) null));
  }
}
