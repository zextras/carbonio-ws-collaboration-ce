// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RoomUserSettingsRepository {

  /**
   * Returns the user settings for the required room if it exists, or else it returns an empty {@link Optional}
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @return Returns the user settings for the room if it exists, or else it returns an empty {@link Optional} {@link
   * RoomUserSettings}
   */
  Optional<RoomUserSettings> getByRoomIdAndUserId(String roomId, String userId);

  /**
   * Returns a map of required rooms settings for the user
   *
   * @param roomsIds list of required rooms identifiers
   * @param userId   user identifier
   * @return {@link Map} with the room id {@link String} as the key and that room settings {@link RoomUserSettings} as
   * the value
   */
  Map<String, RoomUserSettings> getMapByRoomsIdsAndUserIdGroupedByRoomsIds(List<String> roomsIds, String userId);

  /**
   * Returns a list of every room settings set by the user
   *
   * @param userId user identifier
   * @return {@link List} of room user settings {@link RoomUserSettings}
   */
  List<RoomUserSettings> getByUserId(String userId);

  /**
   * Returns a map with every room setting set by the user
   *
   * @param userId user identifier
   * @return {@link Map} with the room id {@link String} as the key and that room settings {@link RoomUserSettings} as the value
   */
  Map<String, RoomUserSettings> getMapGroupedByUserId(String userId);

  /**
   * Returns a list of all user setting for a room
   *
   * @param roomId room identifier
   * @return list {@link List} of user settings {@link RoomUserSettings}
   */
  List<RoomUserSettings> getByRoomId(String roomId);

  /**
   * Deletes a user settings
   *
   * @param userSettings user settings to delete {@link RoomUserSettings}
   */
  void delete(RoomUserSettings userSettings);

  /**
   * Saves a room user settings (insert or update)
   *
   * @param roomUserSettings room user settings to save {@link RoomUserSettings}
   * @return saved room user settings {@link RoomUserSettings}
   */
  RoomUserSettings save(RoomUserSettings roomUserSettings);

  /**
   * Saves a collection of room user settings
   *
   * @param roomUserSettingsList collection {@link Collection} of room user settings to save {@link RoomUserSettings}
   */
  void save(Collection<RoomUserSettings> roomUserSettingsList);

  /**
   * Returns the highest workspace rank for the specified user
   *
   * @param userId user identifier
   * @return the highest workspace rank for the user
   */
  Optional<Integer> getWorkspaceMaxRank(String userId);

  /**
   * Returns the highest workspace rank for the specified users list
   *
   * @param userIds users identifiers list
   * @return {@link Map} with the user id {@link String} as the key and that the highest workspace rank room settings {@link RoomUserSettings} as the value
   */
  Map<String, RoomUserSettings> getWorkspaceMaxRanksMapGroupedByUsers(List<String> userIds);

  /**
   * Returns a map of every workspace settings set by the user
   *
   * @param userId user identifier
   * @return {@link Map} with the workspace id {@link String} as the key and that room settings {@link RoomUserSettings} as the value
   */
  Map<String, RoomUserSettings> getWorkspaceMapGroupedByRoomId(String userId);
}
