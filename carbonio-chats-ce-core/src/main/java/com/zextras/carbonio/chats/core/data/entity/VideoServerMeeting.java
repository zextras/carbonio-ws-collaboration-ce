// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "VIDEOSERVER_MEETING", schema = "CHATS")
public class VideoServerMeeting {

  @Id
  @Column(name = "MEETING_ID", length = 64, nullable = false)
  private String meetingId;

  @Column(name = "CONNECTION_ID", length = 64, nullable = false)
  private String connectionId;

  @Column(name = "AUDIO_HANDLE_ID", length = 64, nullable = false)
  private String audioHandleId;

  @Column(name = "VIDEO_HANDLE_ID", length = 64, nullable = false)
  private String videoHandleId;

  @Column(name = "AUDIO_ROOM_ID", length = 64, nullable = false)
  private String audioRoomId;

  @Column(name = "VIDEO_ROOM_ID", length = 64, nullable = false)
  private String videoRoomId;

  @OneToMany(mappedBy = "videoServerMeeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<VideoServerSession> videoServerSessions;

  public static VideoServerMeeting create() {
    return new VideoServerMeeting();
  }

  public String getMeetingId() {
    return meetingId;
  }

  public VideoServerMeeting meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public VideoServerMeeting connectionId(String connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  public String getAudioHandleId() {
    return audioHandleId;
  }

  public VideoServerMeeting audioHandleId(String audioHandleId) {
    this.audioHandleId = audioHandleId;
    return this;
  }

  public String getVideoHandleId() {
    return videoHandleId;
  }

  public VideoServerMeeting videoHandleId(String videoHandleId) {
    this.videoHandleId = videoHandleId;
    return this;
  }

  public String getAudioRoomId() {
    return audioRoomId;
  }

  public VideoServerMeeting audioRoomId(String audioRoomId) {
    this.audioRoomId = audioRoomId;
    return this;
  }

  public String getVideoRoomId() {
    return videoRoomId;
  }

  public VideoServerMeeting videoRoomId(String videoRoomId) {
    this.videoRoomId = videoRoomId;
    return this;
  }

  public List<VideoServerSession> getVideoServerSessions() {
    return videoServerSessions;
  }

  public VideoServerMeeting videoServerSessions(List<VideoServerSession> videoServerSessions) {
    this.videoServerSessions = videoServerSessions;
    return this;
  }
}
