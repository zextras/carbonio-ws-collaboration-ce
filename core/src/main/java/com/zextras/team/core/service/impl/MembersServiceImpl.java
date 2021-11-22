package com.zextras.team.core.service.impl;

import com.zextras.team.core.data.entity.Subscription;
import com.zextras.team.core.exception.NotFoundException;
import com.zextras.team.core.repository.SubscriptionRepository;
import com.zextras.team.core.service.MembersService;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;

public class MembersServiceImpl implements MembersService {

  private final SubscriptionRepository subscriptionRepository;

  @Inject
  public MembersServiceImpl(SubscriptionRepository subscriptionRepository) {
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public void modifyOwner(UUID roomId, UUID userId, boolean isOwner, SecurityContext securityContext) {
    // TODO: 19/11/21 current user validation
    Subscription subscription = subscriptionRepository.getById(roomId.toString(), userId.toString())
      .orElseThrow(() -> new NotFoundException("Subscription not found"));
    subscription.owner(isOwner);
    subscriptionRepository.save(subscription);
  }
}
