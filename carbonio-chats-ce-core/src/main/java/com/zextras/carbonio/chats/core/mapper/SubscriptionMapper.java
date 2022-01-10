package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.model.MemberDto;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jsr330", imports = {UUID.class})
public abstract class SubscriptionMapper {

  @Mapping(target = "userId", expression = "java(UUID.fromString(subscription.getUserId()))")
  public abstract MemberDto ent2memberDto(Subscription subscription);

  public abstract List<MemberDto> ent2memberDto(List<Subscription> subscription);

}
