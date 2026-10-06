// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "meeting", schema = "chats")
public class MeetingEntity {
  @Id public String id;

  @Column(name = "room_id", nullable = false)
  public String roomId;

  public String name;

  @Column(name = "meeting_type", nullable = false)
  public String meetingType;

  public OffsetDateTime expiration;
  public Boolean active;

  @Column(name = "started_at")
  public OffsetDateTime startedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  public OffsetDateTime createdAt;
}
