package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
public class JanusRoomErrorResponse implements JanusPluginResponse {

  @JsonProperty
  private String videoRoom;
  @JsonProperty
  private String audioRoom;
  @JsonProperty
  private String errorCode;
  @JsonProperty
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

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return this.toString();
    }
  }
}
