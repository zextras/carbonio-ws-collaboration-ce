// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.model.MemberDto;
import jakarta.annotation.Nullable;
import java.util.List;

public interface SubscriptionMapper {

  /**
   * Converts {@link Subscription} to {@link MemberDto}
   *
   * @param subscription {@link Subscription} to convert
   * @return conversation result {@link MemberDto}
   */
  @Nullable
  MemberDto ent2memberDto(@Nullable Subscription subscription);

  /**
   * Converts a {@link List} of {@link Subscription} to a {@link List} of {@link MemberDto}
   *
   * @param subscriptions {@link List} of {@link Subscription} to convert
   * @return conversation result ({@link List} of {@link MemberDto})
   */
  List<MemberDto> ent2memberDto(@Nullable List<Subscription> subscriptions);
}
