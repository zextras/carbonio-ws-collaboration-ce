// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "jsr330", imports = {ArrayList.class, UUID.class, RoomUserSettingsDto.class, Collectors.class})
public abstract class RoomMapper {

  @Inject
  protected SubscriptionMapper subscriptionMapper;

  @Inject
  protected RoomUserSettingsMapper roomUserSettingsMapper;

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))"),
    @Mapping(target = "rank", expression = "java(getRank(room, null))"),
    @Mapping(target = "members", expression = "java(includeMembers ? subscriptionMapper.ent2memberDto(room.getSubscriptions()) : null)"),
    @Mapping(target = "userSettings", ignore = true)
  })
  public abstract RoomDto ent2roomDto(@Nullable Room room, boolean includeMembers);

  public List<RoomDto> ent2roomDto(
    @Nullable List<Room> rooms, boolean includeMembers, @Nullable Map<String, RoomUserSettings> settingsMap
  ) {
    if (rooms == null) {
      return null;
    }
    return rooms.stream()
      .map(room -> ent2roomDto(room, includeMembers)
        .userSettings(settingsMap == null ? null : roomUserSettingsMapper.ent2dto(settingsMap.get(room.getId())))
      ).collect(Collectors.toList());
  }

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))"),
    @Mapping(target = "rank", expression = "java(getRank(room, null))"),
    @Mapping(target = "members", expression = "java(subscriptionMapper.ent2memberDto(room.getSubscriptions()))"),
    @Mapping(target = "userSettings", expression = "java(roomUserSettingsMapper.ent2dto(room.getUserSettings()))")
  })
  public abstract RoomInfoDto ent2roomInfoDto(Room room);

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))"),
    @Mapping(target = "rank", expression = "java(getRank(room, userId))"),
    @Mapping(target = "members", expression = "java(subscriptionMapper.ent2memberDto(room.getSubscriptions()))"),
    @Mapping(target = "userSettings", expression = "java(roomUserSettingsMapper.ent2dto(room.getUserSettings().stream().filter(us -> us.getUserId().equals(userId.toString())).collect(Collectors.toList())))"),
  })
  public abstract RoomInfoDto ent2roomInfoDto(Room room, UUID userId);

  @Nullable
  protected Integer getRank(Room room, @Nullable UUID userId) {
    if (RoomTypeDto.WORKSPACE.equals(room.getType()) && room.getUserSettings() != null
      && room.getUserSettings().size() > 0) {
      if (userId != null) {
        Optional<RoomUserSettings> userSettings = room.getUserSettings().stream()
          .filter(us -> us.getUserId().equals(userId.toString())).findAny();
        if (userSettings.isPresent()) {
          return userSettings.get().getRank();
        }
      } else {
        return room.getUserSettings().get(0).getRank();
      }
    }
    return null;
  }
}
