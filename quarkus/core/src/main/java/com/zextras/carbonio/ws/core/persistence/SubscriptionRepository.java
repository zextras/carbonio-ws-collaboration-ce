// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SubscriptionRepository
    implements PanacheRepositoryBase<SubscriptionEntity, SubscriptionId> {

  public Optional<SubscriptionEntity> find(String roomId, String userId) {
    return findByIdOptional(new SubscriptionId(userId, roomId));
  }

  public List<SubscriptionEntity> listByRoom(String roomId) {
    return list("roomId", roomId);
  }
}
