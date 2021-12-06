package com.zextras.chats.core.repository.impl;

import com.zextras.chats.core.data.entity.Subscription;
import com.zextras.chats.core.data.entity.SubscriptionId;
import com.zextras.chats.core.repository.SubscriptionRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Transactional
@Singleton
public class EbeanSubscriptionRepository implements SubscriptionRepository {

  private final Database db;

  @Inject
  public EbeanSubscriptionRepository(Database db) {
    this.db = db;
  }

  public Optional<Subscription> getById(String roomId, String userId) {
    return Optional.ofNullable(db.find(Subscription.class, new SubscriptionId(userId, roomId)));
  }

  @Override
  public Subscription update(Subscription subscription) {
    db.update(subscription);
    return subscription;
  }

  @Override
  public Subscription insert(Subscription subscription) {
    if(subscription.getId() == null) {
      subscription.id(new SubscriptionId(subscription.getRoom().getId(), subscription.getUserId()));
    }
    db.insert(subscription);
    return subscription;
  }

  @Override
  public void delete(String roomId, String userId) {
    db.delete(Subscription.class, new SubscriptionId(roomId, userId));
  }
}
