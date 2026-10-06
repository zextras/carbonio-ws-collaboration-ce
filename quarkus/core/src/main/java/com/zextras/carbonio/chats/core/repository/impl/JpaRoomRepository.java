// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.model.RoomTypeDto;
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
public class JpaRoomRepository implements RoomRepository {

  private final EntityManager em;

  @Inject
  public JpaRoomRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public List<String> getIdsByUserId(String userId) {
    return em.createQuery(
            "SELECT DISTINCT s.id.roomId FROM Subscription s WHERE s.id.userId = :userId",
            String.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public List<Room> getByUserId(String userId, boolean withSubscriptions) {
    // subscriptions are always loaded (eager), so withSubscriptions needs no special handling
    return em.createQuery(
            "SELECT r FROM Room r WHERE EXISTS (SELECT 1 FROM Subscription s"
                + " WHERE s.room = r AND s.id.userId = :userId)",
            Room.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public Optional<Room> getById(String roomId) {
    return Optional.ofNullable(em.find(Room.class, roomId));
  }

  @Override
  public Optional<Room> getOneToOneByAllUserIds(String user1Id, String user2Id) {
    return em.createQuery(
            "SELECT r FROM Room r WHERE r.type = :type"
                + " AND EXISTS (SELECT 1 FROM Subscription a WHERE a.room = r AND a.id.userId = :u1)"
                + " AND EXISTS (SELECT 1 FROM Subscription b WHERE b.room = r AND b.id.userId = :u2)",
            Room.class)
        .setParameter("type", RoomTypeDto.ONE_TO_ONE)
        .setParameter("u1", user1Id)
        .setParameter("u2", user2Id)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Room insert(Room room) {
    em.persist(room);
    return room;
  }

  @Override
  public Room update(Room room) {
    return em.merge(room);
  }

  @Override
  public void delete(String roomId) {
    Room room = em.find(Room.class, roomId);
    if (room != null) {
      em.remove(room);
    }
  }
}
