// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "videoserver_meeting", schema = "chats")
public class VideoServerMeetingEntity {
  @Id
  @Column(name = "meeting_id")
  public String meetingId;

  @Column(name = "server_id")
  public String serverId;

  @Column(name = "connection_id", nullable = false)
  public String connectionId;

  @Column(name = "audio_handle_id", nullable = false)
  public String audioHandleId;

  @Column(name = "video_handle_id", nullable = false)
  public String videoHandleId;

  @Column(name = "audio_room_id", nullable = false)
  public String audioRoomId;

  @Column(name = "video_room_id", nullable = false)
  public String videoRoomId;
}
