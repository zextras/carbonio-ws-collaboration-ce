// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.model.RoomDto;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface RoomMapper {

  /**
   * Converts {@link Room} to {@link RoomDto}
   *
   * @param room {@link Room} to convert
   * @param userSettings current user settings {@link RoomUserSettings}
   * @param includeMembers if true, it includes the room members
   * @param includeSettings if true, it includes the current user settings
   * @return Conversation result {@link RoomDto}
   */
  @Nullable
  RoomDto ent2dto(
      @Nullable Room room,
      @Nullable RoomUserSettings userSettings,
      boolean includeMembers,
      boolean includeSettings);

  /**
   * Converts {@link List} of {@link Room} to {@link List} of {@link RoomDto}
   *
   * @param rooms {@link List} of {@link Room} to convert
   * @param userSettings current user settings {@link RoomUserSettings}
   * @param includeMembers if true, it includes the room members
   * @param includeSettings if true, it includes the current user settings
   * @return Conversation result ({@link List} of {@link RoomDto})
   */
  List<RoomDto> ent2dto(
      @Nullable List<Room> rooms,
      @Nullable RoomUserSettings userSettings,
      boolean includeMembers,
      boolean includeSettings);

  /**
   * Converts {@link List} of {@link Room} to {@link List} of {@link RoomDto}
   *
   * @param rooms {@link List} of {@link Room} to convert
   * @param settingsMapByRoomId current user settings {@link Map} for {@link RoomUserSettings}
   * @param includeMembers if true, it includes the room members
   * @param includeSettings if true, it includes the current user settings
   * @return Conversation result ({@link List} of {@link RoomDto})
   */
  List<RoomDto> ent2dto(
      @Nullable List<Room> rooms,
      @Nullable Map<String, RoomUserSettings> settingsMapByRoomId,
      boolean includeMembers,
      boolean includeSettings);
}
