package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class JanusRoomErrorResponse implements JanusPluginResponse {

  private String videoRoom;
  private String audioRoom;
  private String errorCode;
  private String error;

  public JanusRoomErrorResponse() {
  }

  public JanusRoomErrorResponse(String videoRoom, String audioRoom, String errorCode, String error) {
    this.videoRoom = videoRoom;
    this.audioRoom = audioRoom;
    this.errorCode = errorCode;
    this.error = error;
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  public void setVideoRoom(String videoRoom) {
    this.videoRoom = videoRoom;
  }

  public String getAudioRoom() {
    return audioRoom;
  }

  public void setAudioRoom(String audioRoom) {
    this.audioRoom = audioRoom;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Override
  public boolean statusOK() {
    return false;
  }

  @Override
  public String getRoom() {
    return null;
  }
}
