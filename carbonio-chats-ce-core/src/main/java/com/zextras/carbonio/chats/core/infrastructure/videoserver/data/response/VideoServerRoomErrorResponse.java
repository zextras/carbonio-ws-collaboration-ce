package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerRoomErrorResponse implements VideoServerPluginResponse {

  @JsonProperty("videoroom")
  private String videoRoom;
  @JsonProperty("audioroom")
  private String audioRoom;
  private String errorCode;
  private String error;

  public String getVideoRoom() {
    return videoRoom;
  }

  public String getAudioRoom() {
    return audioRoom;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getError() {
    return error;
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
