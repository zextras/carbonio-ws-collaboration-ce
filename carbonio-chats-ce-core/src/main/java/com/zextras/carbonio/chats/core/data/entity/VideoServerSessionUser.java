package com.zextras.carbonio.chats.core.data.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "VIDEOSERVER_SESSION_USER", schema = "CHATS")
public class VideoServerSessionUser {

  @EmbeddedId
  private VideoServerSessionUserId id;

  @MapsId("userId")
  private String userId;

  @MapsId("sessionId")
  private String sessionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("meetingId")
  @JoinColumn(name = "MEETING_ID")
  private VideoServerMeeting videoServerMeeting;

  @Column(name = "CONNECTION_ID", length = 64, nullable = false)
  private String connectionId;

  @Column(name = "AUDIO_HANDLE_ID", length = 64, nullable = false)
  private String audioHandleId;

  @Column(name = "VIDEO_HANDLE_ID", length = 64, nullable = false)
  private String videoHandleId;

  @Column(name = "AUDIO_ON")
  private Boolean audioOn;

  @Column(name = "VIDEO_ON")
  private Boolean videoOn;

  public VideoServerSessionUser() {
    this.id = VideoServerSessionUserId.create();
  }

  public VideoServerSessionUser(String userId, String sessionId, VideoServerMeeting videoServerMeeting) {
    this.id = VideoServerSessionUserId.create(userId, sessionId, videoServerMeeting.getMeetingId());
    this.userId = userId;
    this.sessionId = sessionId;
    this.videoServerMeeting = videoServerMeeting;
  }

  public static VideoServerSessionUser create() {
    return new VideoServerSessionUser();
  }

  public static VideoServerSessionUser create(String userId, String sessionId, VideoServerMeeting videoServerMeeting) {
    return new VideoServerSessionUser(userId, sessionId, videoServerMeeting);
  }

  public VideoServerSessionUserId getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public VideoServerSessionUser userId(String userId) {
    this.id.userId(userId);
    this.userId = userId;
    return this;
  }

  public String getSessionId() {
    return sessionId;
  }

  public VideoServerSessionUser sessionId(String sessionId) {
    this.id.sessionId(sessionId);
    this.sessionId = sessionId;
    return this;
  }

  public VideoServerMeeting getVideoServerMeeting() {
    return videoServerMeeting;
  }

  public VideoServerSessionUser videoServerMeeting(VideoServerMeeting videoServerMeeting) {
    this.videoServerMeeting = videoServerMeeting;
    this.id.meetingId(videoServerMeeting.getMeetingId());
    return this;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public VideoServerSessionUser connectionId(String connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  public String getAudioHandleId() {
    return audioHandleId;
  }

  public VideoServerSessionUser audioHandleId(String audioHandleId) {
    this.audioHandleId = audioHandleId;
    return this;
  }

  public String getVideoHandleId() {
    return videoHandleId;
  }

  public VideoServerSessionUser videoHandleId(String videoHandleId) {
    this.videoHandleId = videoHandleId;
    return this;
  }

  public Boolean isAudioOn() {
    return audioOn;
  }

  public VideoServerSessionUser audioOn(Boolean audioOn) {
    this.audioOn = audioOn;
    return this;
  }

  public Boolean isVideoOn() {
    return videoOn;
  }

  public VideoServerSessionUser videoOn(Boolean videoOn) {
    this.videoOn = videoOn;
    return this;
  }
}
