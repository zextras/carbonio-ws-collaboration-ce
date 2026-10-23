// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanSubscriptionRepository implements SubscriptionRepository {

  private static final String VISIBILITY_FLOOR_SQL =
      """
      SELECT rus.cleared_at
      FROM chats.subscription s
      LEFT JOIN chats.room_user_settings rus ON rus.room_id = s.room_id AND rus.user_id = s.user_id
      WHERE s.room_id = :roomId AND s.user_id = :userId
      """;

  private final Database db;

  @Inject
  public EbeanSubscriptionRepository(Database db) {
    this.db = db;
  }

  public Optional<Subscription> getById(String roomId, String userId) {
    return Optional.ofNullable(db.find(Subscription.class, new SubscriptionId(roomId, userId)));
  }

  @Override
  public Optional<VisibilityFloor> getVisibilityFloor(String roomId, String userId) {
    return db.sqlQuery(VISIBILITY_FLOOR_SQL)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .findOneOrEmpty()
        .map(
            row ->
                new VisibilityFloor(
                    Optional.ofNullable(row.getTimestamp("cleared_at"))
                        .map(ts -> ts.toInstant().atOffset(ZoneOffset.UTC))
                        .orElse(null)));
  }

  @Override
  public Subscription update(Subscription subscription) {
    db.update(subscription);
    return subscription;
  }

  @Override
  public List<Subscription> updateAll(List<Subscription> subscriptions) {
    db.updateAll(subscriptions);
    return subscriptions;
  }

  @Override
  public Subscription insert(Subscription subscription) {
    if (subscription.getId() == null) {
      subscription.id(new SubscriptionId(subscription.getRoom().getId(), subscription.getUserId()));
    }
    db.insert(subscription);
    return subscription;
  }

  @Override
  public void delete(String roomId, String userId) {
    db.delete(Subscription.class, new SubscriptionId(roomId, userId));
  }

  @Override
  public List<String> getUserIdsByRoomId(String roomId) {
    return db.sqlQuery("SELECT user_id FROM chats.subscription WHERE room_id = :roomId")
        .setParameter("roomId", roomId)
        .mapToScalar(String.class)
        .findList();
  }

  @Override
  @Transactional
  public List<String> getContacts(String userId) {
    return db.createQuery(Subscription.class)
        .setDistinct(true)
        .select("userId")
        .where()
        .in(
            "id.roomId",
            db.createQuery(Subscription.class)
                .select("id.roomId")
                .where()
                .eq("userId", userId)
                .query())
        .findSingleAttributeList();
  }
}
