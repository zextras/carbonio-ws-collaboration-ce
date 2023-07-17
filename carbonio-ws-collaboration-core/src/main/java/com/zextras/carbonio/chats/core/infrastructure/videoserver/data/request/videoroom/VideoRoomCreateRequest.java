// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to create a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomCreateRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomCreateRequest extends VideoRoomRequest {

  public static final String CREATE                 = "create";
  public static final String ROOM_DEFAULT           = "video_";
  public static final String DESCRIPTION_DEFAULT    = "video_room_";
  public static final int    MAX_PUBLISHERS_DEFAULT = 100;
  public static final long   BITRATE_DEFAULT        = 200L;

  private String       request;
  private String       room;
  private Boolean      permanent;
  private String       description;
  private String       secret;
  private String       pin;
  private Boolean      isPrivate;
  private List<String> allowed;
  @JsonProperty("require_pvtid")
  private Boolean      requirePvtId;
  private Boolean      signedTokens;
  private Integer      publishers;
  private Long         bitrate;
  private Boolean      bitrateCap;
  private Integer      firFreq;
  @JsonProperty("audiocodec")
  private String       audioCodec;
  @JsonProperty("videocodec")
  private String       videoCodec;
  private String       vp9Profile;
  private String       h264Profile;
  private Boolean      opusFec;
  private Boolean      opusDtx;
  @JsonProperty("audiolevel_ext")
  private Boolean      audioLevelExt;
  @JsonProperty("audiolevel_event")
  private Boolean      audioLevelEvent;
  private Integer      audioLevelAverage;
  @JsonProperty("videoorient_ext")
  private Boolean      videoOrientExt;
  @JsonProperty("playoutdelay_ext")
  private Boolean      playOutDelayExt;
  private Boolean      transportWideCcExt;
  private Boolean      record;
  private String       recordDir;
  private Boolean      lockRecord;
  private Boolean      notifyJoining;
  private Boolean      requireE2ee;
  private Boolean      dummyPublisher;
  private List<String> dummyStreams;

  public static VideoRoomCreateRequest create() {
    return new VideoRoomCreateRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomCreateRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomCreateRequest room(String room) {
    this.room = room;
    return this;
  }

  public Boolean isPermanent() {
    return permanent;
  }

  public VideoRoomCreateRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public VideoRoomCreateRequest description(String description) {
    this.description = description;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public VideoRoomCreateRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getPin() {
    return pin;
  }

  public VideoRoomCreateRequest pin(String pin) {
    this.pin = pin;
    return this;
  }

  public Boolean isPrivate() {
    return isPrivate;
  }

  public VideoRoomCreateRequest isPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public VideoRoomCreateRequest allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }

  public Boolean getRequirePvtId() {
    return requirePvtId;
  }

  public VideoRoomCreateRequest requirePvtId(boolean requirePvtId) {
    this.requirePvtId = requirePvtId;
    return this;
  }

  public Boolean isSignedTokens() {
    return signedTokens;
  }

  public VideoRoomCreateRequest signedTokens(boolean signedTokens) {
    this.signedTokens = signedTokens;
    return this;
  }

  public Integer getPublishers() {
    return publishers;
  }

  public VideoRoomCreateRequest publishers(int publishers) {
    this.publishers = publishers;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public VideoRoomCreateRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Boolean getBitrateCap() {
    return bitrateCap;
  }

  public VideoRoomCreateRequest bitrateCap(boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
    return this;
  }

  public Integer getFirFreq() {
    return firFreq;
  }

  public VideoRoomCreateRequest firFreq(int firFreq) {
    this.firFreq = firFreq;
    return this;
  }

  public String getAudioCodec() {
    return audioCodec;
  }

  public VideoRoomCreateRequest audioCodec(String audioCodec) {
    this.audioCodec = audioCodec;
    return this;
  }

  public String getVideoCodec() {
    return videoCodec;
  }

  public VideoRoomCreateRequest videoCodec(String videoCodec) {
    this.videoCodec = videoCodec;
    return this;
  }

  public String getVp9Profile() {
    return vp9Profile;
  }

  public VideoRoomCreateRequest vp9Profile(String vp9Profile) {
    this.vp9Profile = vp9Profile;
    return this;
  }

  public String getH264Profile() {
    return h264Profile;
  }

  public VideoRoomCreateRequest h264Profile(String h264Profile) {
    this.h264Profile = h264Profile;
    return this;
  }

  public Boolean getOpusFec() {
    return opusFec;
  }

  public VideoRoomCreateRequest opusFEC(boolean opusFec) {
    this.opusFec = opusFec;
    return this;
  }

  public Boolean isOpusDtx() {
    return opusDtx;
  }

  public VideoRoomCreateRequest opusDtx(boolean opusDtx) {
    this.opusDtx = opusDtx;
    return this;
  }

  public Boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public VideoRoomCreateRequest audioLevelExt(boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
    return this;
  }

  public Boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public VideoRoomCreateRequest audioLevelEvent(boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomCreateRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Boolean getVideoOrientExt() {
    return videoOrientExt;
  }

  public VideoRoomCreateRequest videoOrientExt(boolean videoOrientExt) {
    this.videoOrientExt = videoOrientExt;
    return this;
  }

  public Boolean getPlayOutDelayExt() {
    return playOutDelayExt;
  }

  public VideoRoomCreateRequest playOutDelayExt(boolean playOutDelayExt) {
    this.playOutDelayExt = playOutDelayExt;
    return this;
  }

  public Boolean getTransportWideCcExt() {
    return transportWideCcExt;
  }

  public VideoRoomCreateRequest transportWideCCExt(boolean transportWideCCExt) {
    this.transportWideCcExt = transportWideCCExt;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public VideoRoomCreateRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public VideoRoomCreateRequest recordDir(String recordDir) {
    this.recordDir = recordDir;
    return this;
  }

  public Boolean getLockRecord() {
    return lockRecord;
  }

  public VideoRoomCreateRequest lockRecord(boolean lockRecord) {
    this.lockRecord = lockRecord;
    return this;
  }

  public Boolean getNotifyJoining() {
    return notifyJoining;
  }

  public VideoRoomCreateRequest notifyJoining(boolean notifyJoining) {
    this.notifyJoining = notifyJoining;
    return this;
  }

  public Boolean getRequireE2ee() {
    return requireE2ee;
  }

  public VideoRoomCreateRequest requireE2ee(boolean requireE2ee) {
    this.requireE2ee = requireE2ee;
    return this;
  }

  public Boolean isDummyPublisher() {
    return dummyPublisher;
  }

  public VideoRoomCreateRequest dummyPublisher(boolean dummyPublisher) {
    this.dummyPublisher = dummyPublisher;
    return this;
  }

  public List<String> getDummyStreams() {
    return dummyStreams;
  }

  public VideoRoomCreateRequest dummyStreams(List<String> dummyStreams) {
    this.dummyStreams = dummyStreams;
    return this;
  }
}
