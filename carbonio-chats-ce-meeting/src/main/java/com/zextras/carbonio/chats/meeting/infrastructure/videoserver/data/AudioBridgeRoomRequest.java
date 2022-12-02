package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

public class AudioBridgeRoomRequest extends AudioBridgeRoom implements RoomRequest {

  @JsonProperty
  private String request;

  //default values
  public AudioBridgeRoomRequest(String request) {
    super("audio_" + UUID.randomUUID(), false, "audio_room_" + UUID.randomUUID(), null, null, false, null, 160000L,
      null,
      null,
      10L, (short) 55, null, null, null, null, null);
    this.request = request;
  }

  public AudioBridgeRoomRequest(String request, String room, Boolean isPermanent, String description, String secret,
    String pin,
    Boolean isPrivate, String allowed, Long samplingRate, Boolean audioLevelExt, Boolean audioLevelEvent,
    Long audioActivePackets, Short audioLevelAverage, Short defaultPreBuffering, Boolean hasToRecord, String recordFile,
    String recordDir, String mjrsDir) {
    super(room, isPermanent, description, secret, pin, isPrivate, allowed, samplingRate, audioLevelExt, audioLevelEvent,
      audioActivePackets, audioLevelAverage, defaultPreBuffering, hasToRecord, recordFile, recordDir, mjrsDir);
    this.request = request;
  }

  public String getRequest() {
    return request;
  }

  public void setRequest(String request) {
    this.request = request;
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
