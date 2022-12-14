package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents an error response provided by VideoServer when creating or editing a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeRoom</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoom</a>
 */
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
