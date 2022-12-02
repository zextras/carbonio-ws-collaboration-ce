package com.zextras.carbonio.chats.core.mapper.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RoomMapperImpl implements RoomMapper {

  private final SubscriptionMapper subscriptionMapper;

  @Inject
  public RoomMapperImpl(SubscriptionMapper subscriptionMapper) {
    this.subscriptionMapper = subscriptionMapper;
  }

  private RoomDto ent2dto(Room room, boolean includeMembers) {
    return RoomDto.create()
      .id(UUID.fromString(room.getId()))
      .name(room.getName())
      .description(room.getDescription())
      .type(room.getType())
      .hash(room.getHash())
      .pictureUpdatedAt(room.getPictureUpdatedAt())
      .createdAt(room.getCreatedAt())
      .updatedAt(room.getUpdatedAt())
      .parentId(room.getParentId() == null ? null : UUID.fromString(room.getParentId()))
      .members(includeMembers ? subscriptionMapper.ent2memberDto(room.getSubscriptions()) : null);
  }

  @Override
  @Nullable
  public RoomDto ent2dto(
    Room room, RoomUserSettings userSettings,
    boolean includeMembers, boolean includeSettings
  ) {
    if (room == null) {
      return null;
    }
    return ent2dto(room, includeMembers)
      .rank(getRank(room, userSettings))
      .children(RoomTypeDto.WORKSPACE.equals(room.getType()) ?
        ent2dto(room.getChildren(), (RoomUserSettings) null, false, includeSettings) :
        null)
      .userSettings(includeSettings ? getRoomUserSettingsDto(room, userSettings) : null);
  }

  @Override
  @Nullable
  public RoomDto ent2dto(
    @Nullable Room room, @Nullable Map<String, RoomUserSettings> settingsMapByRoomId,
    boolean includeMembers, boolean includeSettings
  ) {
    if (room == null) {
      return null;
    }
    return ent2dto(room, includeMembers)
      .rank(getRank(room, settingsMapByRoomId == null ? null : settingsMapByRoomId.get(room.getId())))
      .children(RoomTypeDto.WORKSPACE.equals(room.getType()) ?
        ent2dto(room.getChildren(), settingsMapByRoomId, false,
          includeSettings) : null)
      .userSettings(
        includeSettings ?
          getRoomUserSettingsDto(room, settingsMapByRoomId == null ? null : settingsMapByRoomId.get(room.getId())) :
          null);
  }

  @Override
  public List<RoomDto> ent2dto(
    @Nullable List<Room> rooms, @Nullable RoomUserSettings userSettings,
    boolean includeMembers, boolean includeSettings
  ) {
    return rooms == null ? List.of() : rooms.stream()
      .map(room -> ent2dto(room, userSettings, includeMembers, includeSettings))
      .collect(Collectors.toList());
  }

  @Override
  public List<RoomDto> ent2dto(
    List<Room> rooms, @Nullable Map<String, RoomUserSettings> settingsMapByRoomId,
    boolean includeMembers, boolean includeSettings
  ) {
    return rooms == null ? List.of() : rooms.stream()
      .map(room -> ent2dto(room, settingsMapByRoomId, includeMembers, includeSettings)
      ).collect(Collectors.toList());
  }

  @Nullable
  private RoomUserSettingsDto getRoomUserSettingsDto(
    Room room, @Nullable RoomUserSettings userSettings
  ) {
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      return null;
    } else {
      RoomUserSettingsDto userSettingsDto = RoomUserSettingsDto.create();
      if (userSettings == null) {
        userSettingsDto.muted(false);
      } else {
        userSettingsDto.muted(userSettings.getMutedUntil() != null);
        userSettingsDto.clearedAt(userSettings.getClearedAt());
      }
      return userSettingsDto;
    }
  }

  @Nullable
  private Integer getRank(Room room, @Nullable RoomUserSettings userSettings) {
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      return userSettings == null ? null : userSettings.getRank();
    } else {
      return room.getRank();
    }
  }
}
