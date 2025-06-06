// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.model.MemberDto;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Singleton
public class SubscriptionMapperImpl implements SubscriptionMapper {

  @Override
  @Nullable
  public MemberDto ent2memberDto(@Nullable Subscription subscription) {
    if (subscription == null) {
      return null;
    }
    return MemberDto.create()
        .userId(UUID.fromString(subscription.getUserId()))
        .owner(subscription.isOwner());
  }

  @Override
  public List<MemberDto> ent2memberDto(@Nullable List<Subscription> subscriptions) {
    return subscriptions == null
        ? List.of()
        : subscriptions.stream().map(this::ent2memberDto).toList();
  }
}
