// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

  /**
   * Gets the subscription by room and user
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @return an {@link Optional} with the {@link Subscription} required if it exists
   */
  Optional<Subscription> getById(String roomId, String userId);

  /**
   * Gets, in one query, the visibility floor of a room member
   *
   * @param roomId room identifier
   * @param userId user identifier
   * @return the {@link VisibilityFloor}, empty when the user is not a member of the room
   */
  Optional<VisibilityFloor> getVisibilityFloor(String roomId, String userId);

  /**
   * Updates a subscription
   *
   * @param subscription {@link Subscription} to update
   * @return {@link Subscription} updated
   */
  Subscription update(Subscription subscription);

  /**
   * Updates a list of subscriptions
   *
   * @param subscriptions {@link List} of {@link Subscription} to update
   * @return {@link List} of {@link Subscription} updated
   */
  List<Subscription> updateAll(List<Subscription> subscriptions);

  /**
   * Inserts a new subscription
   *
   * @param subscription {@link Subscription} to insert
   * @return {@link Subscription} inserted
   */
  Subscription insert(Subscription subscription);

  /**
   * Deletes a subscription by its identifier
   *
   * @param roomId room identifier {@link String}
   * @param userId user identifier {@link String}
   */
  void delete(String roomId, String userId);

  /**
   * Gets all users who share a room with specified user
   *
   * @param userId user identifier
   * @return {@link List} of users identifier {@link String}
   */
  List<String> getContacts(String userId);

  /**
   * Gets the identifiers of the users subscribed to the room
   *
   * @param roomId room identifier
   * @return {@link List} of users identifier {@link String}
   */
  List<String> getUserIdsByRoomId(String roomId);
}
