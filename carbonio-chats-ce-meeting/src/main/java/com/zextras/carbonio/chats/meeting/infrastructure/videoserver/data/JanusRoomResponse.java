package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public class JanusRoomResponse implements JanusPluginResponse {

  @JsonProperty
  private String            audioRoom;
  @JsonProperty
  private String            videoRoom;
  @JsonProperty
  private String            room;
  @JsonProperty
  private String            permanent;
  @JsonProperty
  private boolean           exists;
  @JsonProperty
  private Allowed           allowed;
  @JsonProperty
  private List<VideoRoom>   videoRoomList;
  @JsonProperty
  private List<Participant> participantList;

  public JanusRoomResponse() {
  }

  public JanusRoomResponse(String audioRoom, String videoRoom, String room, String permanent, boolean exists,
    Allowed allowed, List<VideoRoom> videoRoomList, List<Participant> participantList) {
    this.videoRoom = videoRoom;
    this.room = room;
    this.permanent = permanent;
    this.exists = exists;
    this.allowed = allowed;
    this.videoRoomList = videoRoomList;
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

  public List<VideoRoom> getVideoRoomList() {
    return videoRoomList;
  }

  public void setVideoRoomList(List<VideoRoom> videoRoomList) {
    this.videoRoomList = videoRoomList;
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

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return this.toString();
    }
  }
}
