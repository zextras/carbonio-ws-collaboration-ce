// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recording", schema = "chats")
public class RecordingEntity {
  @Id public String id;

  @Column(name = "meeting_id", nullable = false)
  public String meetingId;

  @Column(name = "starter_id", nullable = false)
  public String starterId;

  @Column(name = "folder_id")
  public String folderId;

  @Column(name = "started_at", nullable = false)
  public OffsetDateTime startedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public RecordingStatus status;
}
