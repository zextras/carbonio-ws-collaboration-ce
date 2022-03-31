// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "jsr330", imports = {ArrayList.class, UUID.class, RoomUserSettingsDto.class})
public abstract class RoomMapper {

  @Inject
  protected SubscriptionMapper subscriptionMapper;

  @Inject
  protected RoomUserSettingsMapper roomUserSettingsMapper;

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))"),
    @Mapping(target = "members", expression =
      "java(includeMembers ? subscriptionMapper.ent2memberDto(room.getSubscriptions()) : null)"),
    @Mapping(target = "userSettings", expression =
      "java(includeSettings ? roomUserSettingsMapper.ent2dto(room.getUserSettings()) : null)")
  })
  public abstract RoomDto ent2roomDto(@Nullable Room room, boolean includeMembers, boolean includeSettings);

  public List<RoomDto> ent2roomDto(List<Room> rooms, boolean includeMembers, boolean includeSettings) {
    if (rooms == null) {
      return null;
    }
    return rooms.stream()
      .map(room -> ent2roomDto(room, includeMembers, includeSettings))
      .collect(Collectors.toList());
  }

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))"),
    @Mapping(target = "members", expression = "java(subscriptionMapper.ent2memberDto(room.getSubscriptions()))"),
    @Mapping(target = "userSettings", expression = "java(roomUserSettingsMapper.ent2dto(room.getUserSettings()))")
  })
  public abstract RoomInfoDto ent2roomInfoDto(Room room);

}
