package com.zextras.team.core.repository;

import com.zextras.team.core.data.entity.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {

  Optional<Subscription> getById(String roomId, String userId);
  Subscription save(Subscription roomSubscription);
}
