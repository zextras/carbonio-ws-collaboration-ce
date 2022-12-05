package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

public class VideoRoomRequest extends VideoRoom implements RoomRequest {

  @JsonProperty
  private String request;

  //default values
  public VideoRoomRequest(String request) {
    super("video_" + UUID.randomUUID(), false, "video_room_" + UUID.randomUUID(), null, null, false, List.of(), null,
      (short) 100, 200L, true,
      null, null, VideoCodec.VP8, null, null, null, null, null, null,
      null, null, null, null, null, null, null,
      null, null);
    this.request = request;
  }

  public VideoRoomRequest(String request, String room, boolean permanent, String description, String secret, String pin,
    boolean isPrivate, List<String> allowed, boolean requirePvtId, short publishers, long bitrate,
    boolean bitrateCap, int firFreq, AudioCodec audioCodec, VideoCodec videoCodec, short vp9Profile, short h264Profile,
    boolean opusFEC, boolean videoSVC, boolean audioLevelExt, boolean audioLevelEvent, short audioLevelAverage,
    boolean videoOrientExt, boolean playoutDelayExt, boolean transportWideCCExt, boolean record, String recordDir,
    boolean lockRecord, boolean notifyJoining, boolean requireE2ee) {
    super(room, permanent, description, secret, pin, isPrivate, allowed, requirePvtId, publishers, bitrate, bitrateCap,
      firFreq, audioCodec, videoCodec, vp9Profile, h264Profile, opusFEC, videoSVC, audioLevelExt, audioLevelEvent,
      audioLevelAverage, videoOrientExt, playoutDelayExt, transportWideCCExt, record, recordDir, lockRecord,
      notifyJoining, requireE2ee);
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
