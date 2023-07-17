// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

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
@Table(name = "VIDEOSERVER_SESSION", schema = "CHATS")
public class VideoServerSession {

  @EmbeddedId
  private VideoServerSessionId id;

  @MapsId("sessionId")
  private String sessionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("meetingId")
  @JoinColumn(name = "MEETING_ID")
  private VideoServerMeeting videoServerMeeting;

  @Column(name = "CONNECTION_ID", length = 64, nullable = false)
  private String connectionId;

  @Column(name = "AUDIO_HANDLE_ID", length = 64)
  private String audioHandleId;

  @Column(name = "VIDEOOUT_HANDLE_ID", length = 64)
  private String videoOutHandleId;

  @Column(name = "VIDEOIN_HANDLE_ID", length = 64)
  private String videoInHandleId;

  @Column(name = "SCREEN_HANDLE_ID", length = 64)
  private String screenHandleId;

  @Column(name = "AUDIO_STREAM_ON")
  private Boolean audioStreamOn = false;

  @Column(name = "VIDEO_OUT_STREAM_ON")
  private Boolean videoOutStreamOn = false;

  @Column(name = "VIDEO_IN_STREAM_ON")
  private Boolean videoInStreamOn = false;

  @Column(name = "SCREEN_STREAM_ON")
  private Boolean screenStreamOn = false;

  public VideoServerSession() {
    this.id = VideoServerSessionId.create();
  }

  public VideoServerSession(String sessionId, VideoServerMeeting videoServerMeeting) {
    this.id = VideoServerSessionId.create(sessionId, videoServerMeeting.getMeetingId());
    this.sessionId = sessionId;
    this.videoServerMeeting = videoServerMeeting;
  }

  public static VideoServerSession create() {
    return new VideoServerSession();
  }

  public static VideoServerSession create(String sessionId, VideoServerMeeting videoServerMeeting) {
    return new VideoServerSession(sessionId, videoServerMeeting);
  }

  public VideoServerSessionId getId() {
    return id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public VideoServerSession sessionId(String sessionId) {
    this.id.sessionId(sessionId);
    this.sessionId = sessionId;
    return this;
  }

  public VideoServerMeeting getVideoServerMeeting() {
    return videoServerMeeting;
  }

  public VideoServerSession videoServerMeeting(VideoServerMeeting videoServerMeeting) {
    this.videoServerMeeting = videoServerMeeting;
    this.id.meetingId(videoServerMeeting.getMeetingId());
    return this;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public VideoServerSession connectionId(String connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  public String getAudioHandleId() {
    return audioHandleId;
  }

  public VideoServerSession audioHandleId(String audioHandleId) {
    this.audioHandleId = audioHandleId;
    return this;
  }

  public String getVideoOutHandleId() {
    return videoOutHandleId;
  }

  public VideoServerSession videoOutHandleId(String videoOutHandleId) {
    this.videoOutHandleId = videoOutHandleId;
    return this;
  }

  public String getVideoInHandleId() {
    return videoInHandleId;
  }

  public VideoServerSession videoInHandleId(String videoInHandleId) {
    this.videoInHandleId = videoInHandleId;
    return this;
  }

  public String getScreenHandleId() {
    return screenHandleId;
  }

  public VideoServerSession screenHandleId(String screenHandleId) {
    this.screenHandleId = screenHandleId;
    return this;
  }

  public Boolean hasAudioStreamOn() {
    return audioStreamOn;
  }

  public VideoServerSession audioStreamOn(Boolean audioStreamOn) {
    this.audioStreamOn = audioStreamOn;
    return this;
  }

  public Boolean hasVideoOutStreamOn() {
    return videoOutStreamOn;
  }

  public VideoServerSession videoOutStreamOn(Boolean videoOutStreamOn) {
    this.videoOutStreamOn = videoOutStreamOn;
    return this;
  }

  public Boolean hasVideoInStreamOn() {
    return videoInStreamOn;
  }

  public VideoServerSession videoInStreamOn(Boolean videoInStreamOn) {
    this.videoInStreamOn = videoInStreamOn;
    return this;
  }

  public Boolean hasScreenStreamOn() {
    return screenStreamOn;
  }

  public VideoServerSession screenStreamOn(Boolean screenStreamOn) {
    this.screenStreamOn = screenStreamOn;
    return this;
  }
}
