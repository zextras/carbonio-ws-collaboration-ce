package com.zextras.chats.core.repository.impl;

import com.zextras.chats.core.data.entity.Subscription;
import com.zextras.chats.core.data.entity.SubscriptionId;
import com.zextras.chats.core.repository.SubscriptionRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
import javax.inject.Inject;

@Transactional
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
  public void update(Subscription subscription) {
    db.update(subscription);
  }
}
