// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
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
public class JpaMeetingRepository implements MeetingRepository {

  private final EntityManager em;

  @Inject
  public JpaMeetingRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<Meeting> getById(String meetingId) {
    return Optional.ofNullable(em.find(Meeting.class, meetingId));
  }

  @Override
  public List<Meeting> getByRoomsIds(List<String> roomsIds) {
    if (roomsIds.isEmpty()) {
      return List.of();
    }
    return em.createQuery("SELECT m FROM Meeting m WHERE m.roomId IN :ids", Meeting.class)
        .setParameter("ids", roomsIds)
        .getResultList();
  }

  @Override
  public Optional<Meeting> getByRoomId(String roomId) {
    return em.createQuery("SELECT m FROM Meeting m WHERE m.roomId = :roomId", Meeting.class)
        .setParameter("roomId", roomId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Meeting insert(Meeting meeting) {
    em.persist(meeting);
    return meeting;
  }

  @Override
  public Meeting update(Meeting meeting) {
    return em.merge(meeting);
  }

  @Override
  public void delete(Meeting meeting) {
    Meeting managed = em.find(Meeting.class, meeting.getId());
    if (managed != null) {
      em.remove(managed);
    }
  }
}
