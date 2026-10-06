// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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
public class JpaRoomUserSettingsRepository implements RoomUserSettingsRepository {

  private final EntityManager em;

  @Inject
  public JpaRoomUserSettingsRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<RoomUserSettings> getByRoomIdAndUserId(String roomId, String userId) {
    return Optional.ofNullable(em.find(RoomUserSettings.class, new SubscriptionId(roomId, userId)));
  }

  @Override
  public List<RoomUserSettings> getByUserId(String userId) {
    return em.createQuery(
            "SELECT s FROM RoomUserSettings s WHERE s.id.userId = :userId", RoomUserSettings.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public Map<String, RoomUserSettings> getMapGroupedByUserId(String userId) {
    return getByUserId(userId).stream()
        .collect(
            Collectors.toMap(s -> s.getId().getRoomId(), Function.identity(), (a, b) -> a));
  }

  @Override
  public List<RoomUserSettings> getByRoomId(String roomId) {
    return em.createQuery(
            "SELECT s FROM RoomUserSettings s WHERE s.id.roomId = :roomId", RoomUserSettings.class)
        .setParameter("roomId", roomId)
        .getResultList();
  }

  @Override
  public void delete(RoomUserSettings userSettings) {
    RoomUserSettings managed = em.find(RoomUserSettings.class, userSettings.getId());
    if (managed != null) {
      if (managed.getRoom() != null && managed.getRoom().getUserSettings() != null) {
        managed.getRoom().getUserSettings().remove(managed);
      }
      em.remove(managed);
    }
  }

  @Override
  public RoomUserSettings save(RoomUserSettings roomUserSettings) {
    if (roomUserSettings.getId() != null
        && em.find(RoomUserSettings.class, roomUserSettings.getId()) == null) {
      em.persist(roomUserSettings);
      return roomUserSettings;
    }
    return em.merge(roomUserSettings);
  }

  @Override
  public void save(Collection<RoomUserSettings> roomUserSettingsList) {
    roomUserSettingsList.forEach(this::save);
  }
}
