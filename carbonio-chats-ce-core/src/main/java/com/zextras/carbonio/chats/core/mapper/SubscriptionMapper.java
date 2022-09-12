// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330", imports = {UUID.class})
public abstract class SubscriptionMapper {

  @Mapping(target = "userId", expression = "java(UUID.fromString(subscription.getUserId()))")
  public abstract MemberDto ent2memberDto(Subscription subscription);

  public abstract List<MemberDto> ent2memberDto(List<Subscription> subscription);

  public MemberInsertedDto ent2memberInsertedDto(Subscription subscription, RoomUserSettings roomUserSettings) {
    if (subscription == null) {
      return null;
    }

    return MemberInsertedDto.create()
      .owner(subscription.isOwner())
      .temporary(subscription.isTemporary())
      .external(subscription.isExternal())
      .userId(UUID.fromString(subscription.getUserId()))
      .clearedAt(roomUserSettings == null ? null : roomUserSettings.getClearedAt());
  }
}
