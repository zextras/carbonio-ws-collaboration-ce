// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
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
public class JpaVideoServerMeetingRepository implements VideoServerMeetingRepository {

  private final EntityManager em;

  @Inject
  public JpaVideoServerMeetingRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<VideoServerMeeting> getById(String meetingId) {
    return Optional.ofNullable(em.find(VideoServerMeeting.class, meetingId));
  }

  @Override
  public VideoServerMeeting insert(VideoServerMeeting videoServerMeeting) {
    em.persist(videoServerMeeting);
    return videoServerMeeting;
  }

  @Override
  public void deleteById(String meetingId) {
    VideoServerMeeting managed = em.find(VideoServerMeeting.class, meetingId);
    if (managed != null) {
      em.remove(managed);
    }
  }

  @Override
  public VideoServerMeeting update(VideoServerMeeting videoServerMeeting) {
    return em.merge(videoServerMeeting);
  }
}
