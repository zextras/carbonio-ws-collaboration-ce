package com.zextras.chats.core.repository;

import com.zextras.chats.core.data.entity.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {

  Optional<Subscription> getById(String roomId, String userId);

  void update(Subscription subscription);

}
