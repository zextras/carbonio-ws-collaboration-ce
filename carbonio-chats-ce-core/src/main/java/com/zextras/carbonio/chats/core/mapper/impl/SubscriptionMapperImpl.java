package com.zextras.carbonio.chats.core.mapper.impl;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Singleton;

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
      .owner(subscription.isOwner())
      .temporary(subscription.isTemporary())
      .external(subscription.isExternal());
  }

  @Override
  public List<MemberDto> ent2memberDto(@Nullable List<Subscription> subscriptions) {
    return subscriptions == null ? List.of() :
      subscriptions.stream().map(this::ent2memberDto).collect(Collectors.toList());
  }

  @Override
  @Nullable
  public MemberInsertedDto ent2memberInsertedDto(
    @Nullable Subscription subscription, @Nullable RoomUserSettings roomUserSettings
  ) {
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
