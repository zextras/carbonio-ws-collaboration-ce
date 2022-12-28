// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import java.util.List;
import javax.annotation.Nullable;

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

  /**
   * Converts {@link Subscription} to {@link MemberInsertedDto} with current user settings
   *
   * @param subscription     {@link Subscription} to convert
   * @param roomUserSettings current user settings {@link RoomUserSettings}
   * @return conversation result {@link MemberInsertedDto}
   */
  @Nullable
  MemberInsertedDto ent2memberInsertedDto(
    @Nullable Subscription subscription, @Nullable RoomUserSettings roomUserSettings
  );
}
