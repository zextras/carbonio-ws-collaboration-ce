// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Hibernate port of CE's Ebean repository. Every method runs in its own transaction unless the
 * caller already has one, mirroring Ebean's implicit per-statement transactions.
 */
@ApplicationScoped
@Transactional
public class JpaSubscriptionRepository implements SubscriptionRepository {

  private final EntityManager em;

  @Inject
  public JpaSubscriptionRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<Subscription> getById(String roomId, String userId) {
    return Optional.ofNullable(em.find(Subscription.class, new SubscriptionId(roomId, userId)));
  }

  @Override
  public Subscription update(Subscription subscription) {
    return em.merge(subscription);
  }

  @Override
  public List<Subscription> updateAll(List<Subscription> subscriptions) {
    return subscriptions.stream().map(em::merge).toList();
  }

  @Override
  public Subscription insert(Subscription subscription) {
    if (subscription.getId() == null) {
      subscription.id(new SubscriptionId(subscription.getRoom().getId(), subscription.getUserId()));
    }
    em.persist(subscription);
    return subscription;
  }

  @Override
  public void delete(String roomId, String userId) {
    Subscription subscription = em.find(Subscription.class, new SubscriptionId(roomId, userId));
    if (subscription != null) {
      if (subscription.getRoom() != null && subscription.getRoom().getSubscriptions() != null) {
        subscription.getRoom().getSubscriptions().remove(subscription);
      }
      em.remove(subscription);
    }
  }

  @Override
  public List<String> getContacts(String userId) {
    return em.createQuery(
            "SELECT DISTINCT s.id.userId FROM Subscription s WHERE s.id.roomId IN"
                + " (SELECT s2.id.roomId FROM Subscription s2 WHERE s2.id.userId = :userId)",
            String.class)
        .setParameter("userId", userId)
        .getResultList();
  }
}
