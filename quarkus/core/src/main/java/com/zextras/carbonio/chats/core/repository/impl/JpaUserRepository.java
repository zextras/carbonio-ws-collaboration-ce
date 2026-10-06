// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.repository.UserRepository;
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
public class JpaUserRepository implements UserRepository {

  private final EntityManager em;

  @Inject
  public JpaUserRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<User> getById(String id) {
    return Optional.ofNullable(em.find(User.class, id));
  }

  @Override
  public List<User> getByIds(List<String> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return em.createQuery("SELECT u FROM User u WHERE u.id IN :ids", User.class)
        .setParameter("ids", ids)
        .getResultList();
  }

  @Override
  public User save(User user) {
    if (user.getId() != null && em.find(User.class, user.getId()) == null) {
      em.persist(user);
      return user;
    }
    return em.merge(user);
  }
}
