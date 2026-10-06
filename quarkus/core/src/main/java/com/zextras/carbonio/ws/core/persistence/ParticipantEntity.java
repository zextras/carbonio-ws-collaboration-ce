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
@Table(name = "participant", schema = "chats")
@IdClass(ParticipantId.class)
public class ParticipantEntity {
  @Id
  @Column(name = "meeting_id")
  public String meetingId;

  @Id
  @Column(name = "user_id")
  public String userId;

  @Column(name = "queue_id")
  public String queueId;

  @Column(name = "audio_stream_on")
  public Boolean audioStreamOn = false;

  @Column(name = "video_stream_on")
  public Boolean videoStreamOn = false;

  @Column(name = "screen_stream_on")
  public Boolean screenStreamOn = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  public OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  public OffsetDateTime updatedAt;
}
