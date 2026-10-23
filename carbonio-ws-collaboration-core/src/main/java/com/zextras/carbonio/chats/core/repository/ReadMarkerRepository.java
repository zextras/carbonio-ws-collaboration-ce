// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadMarkerRepository {

  /**
   * Gets the read marker for a user in a room
   *
   * @param userId user identifier
   * @param roomId room identifier
   * @return The {@link ReadMarker} wrapped in an {@link Optional}
   */
  Optional<ReadMarker> get(String userId, String roomId);

  /**
   * Gets all read markers for a room (Smart Markers)
   *
   * @param roomId room identifier
   * @return {@link List} of {@link ReadMarker}
   */
  List<ReadMarker> getByRoomId(String roomId);

  /**
   * Gets all read markers for multiple rooms in a single query
   *
   * @param roomIds list of room identifiers
   * @return {@link List} of {@link ReadMarker}
   */
  List<ReadMarker> getByRoomIds(List<String> roomIds);

  /**
   * Gets all read markers for a user across all rooms
   *
   * @param userId user identifier
   * @return {@link List} of {@link ReadMarker}
   */
  List<ReadMarker> getByUserId(String userId);

  /**
   * Moves the read marker forward atomically; it never moves backwards
   *
   * @param userId user identifier
   * @param roomId room identifier
   * @param at creation time of the last read item
   * @return the new marker time, or empty when the marker did not move (same or older item), in
   *     which case nothing is written
   */
  Optional<OffsetDateTime> advance(String userId, String roomId, OffsetDateTime at);

  /**
   * Deletes read markers for a room
   *
   * @param roomId room identifier
   */
  void deleteByRoomId(String roomId);
}
