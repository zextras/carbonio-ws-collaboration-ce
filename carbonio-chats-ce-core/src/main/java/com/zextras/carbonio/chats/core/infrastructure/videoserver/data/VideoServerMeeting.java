package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import java.util.HashMap;
import java.util.Map;

public class VideoServerMeeting {

  private String                                 backendSessionId;
  private String                                 audioRoomHandleId;
  private String                                 videoRoomHandleId;
  private String                                 audioBridgeRoomId;
  private String                                 videoRoomId;
  private Map<String, VideoServerSessionManager> videoServerSessionManagerMap;

  public VideoServerMeeting() {
  }

  public VideoServerMeeting(String backendSessionId, String audioRoomHandleId, String videoRoomHandleId,
    String audioBridgeRoomId, String videoRoomId) {
    this.backendSessionId = backendSessionId;
    this.audioRoomHandleId = audioRoomHandleId;
    this.videoRoomHandleId = videoRoomHandleId;
    this.audioBridgeRoomId = audioBridgeRoomId;
    this.videoRoomId = videoRoomId;
    this.videoServerSessionManagerMap = new HashMap<>();
  }

  public String getBackendSessionId() {
    return backendSessionId;
  }

  public void setBackendSessionId(String backendSessionId) {
    this.backendSessionId = backendSessionId;
  }

  public String getAudioRoomHandleId() {
    return audioRoomHandleId;
  }

  public void setAudioRoomHandleId(String audioRoomHandleId) {
    this.audioRoomHandleId = audioRoomHandleId;
  }

  public String getVideoRoomHandleId() {
    return videoRoomHandleId;
  }

  public void setVideoRoomHandleId(String videoRoomHandleId) {
    this.videoRoomHandleId = videoRoomHandleId;
  }

  public String getAudioBridgeRoomId() {
    return audioBridgeRoomId;
  }

  public void setAudioBridgeRoomId(String audioBridgeRoomId) {
    this.audioBridgeRoomId = audioBridgeRoomId;
  }

  public String getVideoRoomId() {
    return videoRoomId;
  }

  public void setVideoRoomId(String videoRoomId) {
    this.videoRoomId = videoRoomId;
  }

  public Map<String, VideoServerSessionManager> getVideoServerSessionManagerMap() {
    return videoServerSessionManagerMap;
  }

  public void setVideoServerSessionManagerMap(
    Map<String, VideoServerSessionManager> videoServerSessionManagerMap) {
    this.videoServerSessionManagerMap = videoServerSessionManagerMap;
  }


}
