// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSessionId;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
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
public class JpaVideoServerSessionRepository implements VideoServerSessionRepository {

  private final EntityManager em;

  @Inject
  public JpaVideoServerSessionRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<VideoServerSession> getById(String userId, String meetingId) {
    return Optional.ofNullable(
        em.find(VideoServerSession.class, new VideoServerSessionId(userId, meetingId)));
  }

  @Override
  public Optional<VideoServerSession> getByConnectionId(String connectionId) {
    return em.createQuery(
            "SELECT s FROM VideoServerSession s WHERE s.connectionId = :connectionId",
            VideoServerSession.class)
        .setParameter("connectionId", connectionId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public List<VideoServerSession> getByMeetingId(String meetingId) {
    return em.createQuery(
            "SELECT s FROM VideoServerSession s WHERE s.id.meetingId = :meetingId",
            VideoServerSession.class)
        .setParameter("meetingId", meetingId)
        .getResultList();
  }

  @Override
  public VideoServerSession insert(VideoServerSession videoServerSession) {
    em.persist(videoServerSession);
    return videoServerSession;
  }

  @Override
  public boolean remove(VideoServerSession videoServerSession) {
    VideoServerSession managed = em.find(VideoServerSession.class, videoServerSession.getId());
    if (managed == null) {
      return false;
    }
    if (managed.getVideoServerMeeting() != null
        && managed.getVideoServerMeeting().getVideoServerSessions() != null) {
      managed.getVideoServerMeeting().getVideoServerSessions().remove(managed);
    }
    em.remove(managed);
    return true;
  }

  @Override
  public VideoServerSession update(VideoServerSession videoServerSession) {
    return em.merge(videoServerSession);
  }
}
