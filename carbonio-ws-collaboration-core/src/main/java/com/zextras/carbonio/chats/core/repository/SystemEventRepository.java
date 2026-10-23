// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SystemEventRepository {

  Optional<SystemEvent> getById(String id);

  List<SystemEvent> getByIds(Collection<String> ids);

  /**
   * Inserts a new system event into the timeline.
   *
   * @param event {@link SystemEvent} to insert
   * @return the persisted {@link SystemEvent}
   */
  SystemEvent insert(SystemEvent event);

  /**
   * Deletes all system events of a room (used by the temporary-room full history purge).
   *
   * @param roomId room identifier
   */
  void deleteByRoomId(String roomId);

  /**
   * Retrieves all system events for a room ordered by {@code createdAt} ascending.
   *
   * @param roomId room identifier
   * @return {@link List} of {@link SystemEvent}
   */
  List<SystemEvent> findByRoomId(String roomId);

  /**
   * Retrieves a bounded page of system events for a room ordered by {@code createdAt} descending.
   * Mirrors the same pattern as {@code EbeanMessageRepository.getByRoomId}.
   *
   * @param roomId room identifier
   * @param before exclusive upper bound (null = no upper bound)
   * @param after exclusive lower bound (null = no lower bound)
   * @param limit maximum number of results
   * @return {@link List} of {@link SystemEvent}
   */
  List<SystemEvent> findByRoomIdBounded(
      String roomId, OffsetDateTime before, OffsetDateTime after, int limit);

  List<SystemEvent> findByRoomIdComposite(
      String roomId, OffsetDateTime before, String beforeId, OffsetDateTime after, int limit);

  List<SystemEvent> findByRoomIdForward(
      String roomId, OffsetDateTime after, String afterId, OffsetDateTime before, int limit);
}
