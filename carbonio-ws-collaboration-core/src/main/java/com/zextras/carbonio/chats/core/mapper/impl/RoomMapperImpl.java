// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        .pictureUpdatedAt(room.getPictureUpdatedAt())
        .meetingId(room.getMeetingId() == null ? null : UUID.fromString(room.getMeetingId()))
        .createdAt(room.getCreatedAt())
        .updatedAt(room.getUpdatedAt())
        .members(
            includeMembers
                ? subscriptionMapper.ent2memberDto(room.getSubscriptions())
                : Collections.emptyList());
  }

  @Override
  @Nullable
  public RoomDto ent2dto(
      Room room, RoomUserSettings userSettings, boolean includeMembers, boolean includeSettings) {
    if (room == null) {
      return null;
    }
    return ent2dto(room, includeMembers)
        .userSettings(includeSettings ? getRoomUserSettingsDto(userSettings) : null);
  }

  @Override
  public List<RoomDto> ent2dto(
      @Nullable List<Room> rooms,
      @Nullable RoomUserSettings userSettings,
      boolean includeMembers,
      boolean includeSettings) {
    return rooms == null
        ? List.of()
        : rooms.stream()
            .map(room -> ent2dto(room, userSettings, includeMembers, includeSettings))
            .collect(Collectors.toList());
  }

  public List<RoomDto> ent2dto(
      List<Room> rooms,
      @Nullable Map<String, RoomUserSettings> settingsMapByRoomId,
      boolean includeMembers,
      boolean includeSettings) {
    return rooms == null
        ? List.of()
        : rooms.stream()
            .map(
                room ->
                    ent2dto(room, includeMembers)
                        .userSettings(
                            includeSettings
                                ? getRoomUserSettingsDto(
                                    settingsMapByRoomId == null
                                        ? null
                                        : settingsMapByRoomId.get(room.getId()))
                                : null))
            .collect(Collectors.toList());
  }

  private RoomUserSettingsDto getRoomUserSettingsDto(@Nullable RoomUserSettings userSettings) {
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
