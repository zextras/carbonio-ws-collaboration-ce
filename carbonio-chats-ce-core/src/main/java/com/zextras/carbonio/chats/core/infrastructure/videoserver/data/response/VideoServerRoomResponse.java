package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.Participant;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.Room;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerRoomResponse implements VideoServerPluginResponse {

  @JsonProperty("audioroom")
  private String            audioRoom;
  @JsonProperty("videoroom")
  private String            videoRoom;
  private String            room;
  private String            permanent;
  private boolean           exists;
  private List<String>      allowed;
  @JsonProperty("list")
  private List<Room>        roomList;
  @JsonProperty("participants")
  private List<Participant> participantList;

  public String getAudioRoom() {
    return audioRoom;
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  @Override
  public String getRoom() {
    return room;
  }

  public String getPermanent() {
    return permanent;
  }

  public boolean isExists() {
    return exists;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public List<Room> getVideoRoomList() {
    return roomList;
  }

  public List<Participant> getParticipantList() {
    return participantList;
  }

  @Override
  public boolean statusOK() {
    return true;
  }
}
