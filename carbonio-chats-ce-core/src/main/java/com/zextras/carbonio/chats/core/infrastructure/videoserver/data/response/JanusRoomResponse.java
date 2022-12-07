package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.Participant;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.Room;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class JanusRoomResponse implements JanusPluginResponse {

  private String            audioRoom;
  private String            videoRoom;
  private String            room;
  private String            permanent;
  private boolean           exists;
  private Allowed           allowed;
  @JsonProperty("list")
  private List<Room>        roomList;
  @JsonProperty("participants")
  private List<Participant> participantList;

  public JanusRoomResponse() {
  }

  public JanusRoomResponse(String audioRoom, String videoRoom, String room, String permanent, boolean exists,
    Allowed allowed, List<Room> roomList, List<Participant> participantList) {
    this.audioRoom = audioRoom;
    this.videoRoom = videoRoom;
    this.room = room;
    this.permanent = permanent;
    this.exists = exists;
    this.allowed = allowed;
    this.roomList = roomList;
    this.participantList = participantList;
  }

  public String getAudioRoom() {
    return audioRoom;
  }

  public void setAudioRoom(String audioRoom) {
    this.audioRoom = audioRoom;
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  public void setVideoRoom(String videoRoom) {
    this.videoRoom = videoRoom;
  }

  @Override
  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public String getPermanent() {
    return permanent;
  }

  public void setPermanent(String permanent) {
    this.permanent = permanent;
  }

  public boolean isExists() {
    return exists;
  }

  public void setExists(boolean exists) {
    this.exists = exists;
  }

  public Allowed getAllowed() {
    return allowed;
  }

  public void setAllowed(Allowed allowed) {
    this.allowed = allowed;
  }

  public List<Room> getVideoRoomList() {
    return roomList;
  }

  public void setVideoRoomList(List<Room> videoRoomList) {
    this.roomList = videoRoomList;
  }

  public List<Participant> getParticipantList() {
    return participantList;
  }

  public void setParticipantList(List<Participant> participantList) {
    this.participantList = participantList;
  }

  @Override
  public boolean statusOK() {
    return true;
  }

  private static class Allowed {

  }
}
