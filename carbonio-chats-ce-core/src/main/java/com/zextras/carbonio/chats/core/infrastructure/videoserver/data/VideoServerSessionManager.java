package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

public class VideoServerSessionManager {

  private String  videoServerSessionId;
  private String  userId;
  private String  audioHandleId;
  private String  videoHandleId;
  private boolean webcamOn;
  private boolean audioOn;

  public VideoServerSessionManager() {
  }

  public VideoServerSessionManager(String videoServerSessionId, String userId, String audioHandleId,
    String videoHandleId,
    boolean webcamOn, boolean audioOn) {
    this.videoServerSessionId = videoServerSessionId;
    this.userId = userId;
    this.audioHandleId = audioHandleId;
    this.videoHandleId = videoHandleId;
    this.webcamOn = webcamOn;
    this.audioOn = audioOn;
  }

  public String getVideoServerSessionId() {
    return videoServerSessionId;
  }

  public void setVideoServerSessionId(String videoServerSessionId) {
    this.videoServerSessionId = videoServerSessionId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAudioHandleId() {
    return audioHandleId;
  }

  public void setAudioHandleId(String audioHandleId) {
    this.audioHandleId = audioHandleId;
  }

  public String getVideoHandleId() {
    return videoHandleId;
  }

  public void setVideoHandleId(String videoHandleId) {
    this.videoHandleId = videoHandleId;
  }

  public boolean isWebcamOn() {
    return webcamOn;
  }

  public void setWebcamOn(boolean webcamOn) {
    this.webcamOn = webcamOn;
  }

  public boolean isAudioOn() {
    return audioOn;
  }

  public void setAudioOn(boolean audioOn) {
    this.audioOn = audioOn;
  }
}
