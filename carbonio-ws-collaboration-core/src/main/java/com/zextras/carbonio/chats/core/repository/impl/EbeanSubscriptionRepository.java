// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanSubscriptionRepository implements SubscriptionRepository {

  private final Database db;

  @Inject
  public EbeanSubscriptionRepository(Database db) {
    this.db = db;
  }

  public Optional<Subscription> getById(String roomId, String userId) {
    return Optional.ofNullable(db.find(Subscription.class, new SubscriptionId(roomId, userId)));
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
