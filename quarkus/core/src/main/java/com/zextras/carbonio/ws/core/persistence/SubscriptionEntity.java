// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "subscription", schema = "chats")
@IdClass(SubscriptionId.class)
public class SubscriptionEntity {
  @Id
  @Column(name = "user_id")
  public String userId;

  @Id
  @Column(name = "room_id")
  public String roomId;

  public Boolean owner = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  public OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  public OffsetDateTime updatedAt;
}
