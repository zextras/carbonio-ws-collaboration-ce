// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

  /**
   * Retrieves all identifiers of the rooms of which the user is subscribed
   *
   * @param userId the user identifier
   * @return rooms identifier {@link List}
   */
  List<String> getIdsByUserId(String userId);

  /**
   * Retrieves all {@link Room} of which the user is subscribed
   *
   * @param userId            user identifier
   * @param withSubscriptions if true, rooms will also have subscriptions {@link Subscription}
   * @return {@link Room} {@link List} of which the user is subscribed
   */
  List<Room> getByUserId(String userId, boolean withSubscriptions);

  /**
   * Retrieves the {@link Room} requested with its subscriptions {@link Subscription}
   *
   * @param roomId room identifier
   * @return The {@link Room} wrapped in a {@link Optional}
   */
  Optional<Room> getById(String roomId);

  /**
   * Retrieves the one-to-one {@link Room} by subscribed users ids
   *
   * @param user1Id first user identifier
   * @param user2Id second user identifier
   * @return The one-to-one {@link Room} wrapped in a {@link Optional}
   */
  Optional<Room> getOneToOneByAllUserIds(String user1Id, String user2Id);

  /**
   * Inserts a new {@link Room}
   *
   * @param room {@link Room} to insert
   * @return {@link Room} inserted
   */
  Room insert(Room room);

  /**
   * Updates the {@link Room} data
   *
   * @param room {@link Room} modified
   * @return {@link Room} updated
   */
  Room update(Room room);

  /**
   * Deletes a {@link Room} by its identifier
   *
   * @param roomId room identifier
   */
  void delete(String roomId);

  /**
   * Calculates the maximum rank of channels into a workspace
   *
   * @param workspaceId workspace identifier
   * @return the maximum rank wrapped in a {@link Optional}
   */
  Optional<Integer> getChannelMaxRanksByWorkspace(String workspaceId);

}
