// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.ParticipantId;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
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
public class JpaParticipantRepository implements ParticipantRepository {

  private final EntityManager em;

  @Inject
  public JpaParticipantRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<Participant> getById(String meetingId, String userId) {
    return Optional.ofNullable(
        em.find(Participant.class, ParticipantId.create(meetingId, userId)));
  }

  @Override
  public Optional<Participant> getByUserId(String userId) {
    return em.createQuery("SELECT p FROM Participant p WHERE p.id.userId = :userId", Participant.class)
        .setParameter("userId", userId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Optional<Participant> getByQueueId(String queueId) {
    return em.createQuery("SELECT p FROM Participant p WHERE p.queueId = :queueId", Participant.class)
        .setParameter("queueId", queueId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public List<Participant> getByMeetingId(String meetingId) {
    return em.createQuery(
            "SELECT p FROM Participant p WHERE p.id.meetingId = :meetingId", Participant.class)
        .setParameter("meetingId", meetingId)
        .getResultList();
  }

  @Override
  public List<Participant> getHandRaisedByMeetingId(String meetingId) {
    return em.createQuery(
            "SELECT p FROM Participant p WHERE p.id.meetingId = :meetingId"
                + " AND p.handRaisedAt IS NOT NULL ORDER BY p.handRaisedAt ASC",
            Participant.class)
        .setParameter("meetingId", meetingId)
        .getResultList();
  }

  @Override
  public Participant insert(Participant participant) {
    em.persist(participant);
    return participant;
  }

  @Override
  public Participant update(Participant participant) {
    return em.merge(participant);
  }

  @Override
  public boolean remove(Participant participant) {
    Participant managed = em.find(Participant.class, participant.getId());
    if (managed == null) {
      return false;
    }
    if (managed.getMeeting() != null && managed.getMeeting().getParticipants() != null) {
      managed.getMeeting().getParticipants().remove(managed);
    }
    em.remove(managed);
    return true;
  }

  @Override
  public void clear(String meetingId) {
    em.createQuery("DELETE FROM Participant p WHERE p.id.meetingId = :meetingId")
        .setParameter("meetingId", meetingId)
        .executeUpdate();
  }
}
