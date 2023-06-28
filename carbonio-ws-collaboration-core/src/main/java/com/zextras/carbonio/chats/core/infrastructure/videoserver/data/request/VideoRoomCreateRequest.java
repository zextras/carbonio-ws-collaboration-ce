// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

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
public class VideoRoomCreateRequest {

  public static final String CREATE                 = "create";
  public static final String ROOM_DEFAULT           = "video_";
  public static final String DESCRIPTION_DEFAULT    = "video_room_";
  public static final int    MAX_PUBLISHERS_DEFAULT = 100;
  public static final long   BITRATE_DEFAULT        = 200L;

  private String       request;
  private String       room;
  private boolean      permanent;
  private String       description;
  private String       secret;
  private String       pin;
  private boolean      isPrivate;
  private List<String> allowed;
  @JsonProperty("require_pvtid")
  private boolean      requirePvtId;
  private boolean      signedTokens;
  private int          publishers;
  private long         bitrate;
  private boolean      bitrateCap;
  private int          firFreq;
  @JsonProperty("audiocodec")
  private String       audioCodec;
  @JsonProperty("videocodec")
  private String       videoCodec;
  private int          vp9Profile;
  private int          h264Profile;
  private boolean      opusFec;
  private boolean      opusDtx;
  @JsonProperty("audiolevel_ext")
  private boolean      audioLevelExt;
  @JsonProperty("audiolevel_event")
  private boolean      audioLevelEvent;
  private int          audioLevelAverage;
  @JsonProperty("videoorient_ext")
  private boolean      videoOrientExt;
  @JsonProperty("playoutdelay_ext")
  private boolean      playOutDelayExt;
  private boolean      transportWideCcExt;
  private boolean      record;
  private String       recordDir;
  private boolean      lockRecord;
  private boolean      notifyJoining;
  private boolean      requireE2ee;
  private boolean      dummyPublisher;
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

  public boolean isPermanent() {
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

  public boolean isPrivate() {
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

  public boolean getRequirePvtId() {
    return requirePvtId;
  }

  public VideoRoomCreateRequest requirePvtId(boolean requirePvtId) {
    this.requirePvtId = requirePvtId;
    return this;
  }

  public boolean isSignedTokens() {
    return signedTokens;
  }

  public VideoRoomCreateRequest signedTokens(boolean signedTokens) {
    this.signedTokens = signedTokens;
    return this;
  }

  public int getPublishers() {
    return publishers;
  }

  public VideoRoomCreateRequest publishers(int publishers) {
    this.publishers = publishers;
    return this;
  }

  public long getBitrate() {
    return bitrate;
  }

  public VideoRoomCreateRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public boolean getBitrateCap() {
    return bitrateCap;
  }

  public VideoRoomCreateRequest bitrateCap(boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
    return this;
  }

  public int getFirFreq() {
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

  public int getVp9Profile() {
    return vp9Profile;
  }

  public VideoRoomCreateRequest vp9Profile(int vp9Profile) {
    this.vp9Profile = vp9Profile;
    return this;
  }

  public int getH264Profile() {
    return h264Profile;
  }

  public VideoRoomCreateRequest h264Profile(int h264Profile) {
    this.h264Profile = h264Profile;
    return this;
  }

  public boolean getOpusFec() {
    return opusFec;
  }

  public VideoRoomCreateRequest opusFEC(boolean opusFec) {
    this.opusFec = opusFec;
    return this;
  }

  public boolean isOpusDtx() {
    return opusDtx;
  }

  public VideoRoomCreateRequest opusDtx(boolean opusDtx) {
    this.opusDtx = opusDtx;
    return this;
  }

  public boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public VideoRoomCreateRequest audioLevelExt(boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
    return this;
  }

  public boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public VideoRoomCreateRequest audioLevelEvent(boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public int getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomCreateRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public boolean getVideoOrientExt() {
    return videoOrientExt;
  }

  public VideoRoomCreateRequest videoOrientExt(boolean videoOrientExt) {
    this.videoOrientExt = videoOrientExt;
    return this;
  }

  public boolean getPlayOutDelayExt() {
    return playOutDelayExt;
  }

  public VideoRoomCreateRequest playOutDelayExt(boolean playOutDelayExt) {
    this.playOutDelayExt = playOutDelayExt;
    return this;
  }

  public boolean getTransportWideCcExt() {
    return transportWideCcExt;
  }

  public VideoRoomCreateRequest transportWideCCExt(boolean transportWideCCExt) {
    this.transportWideCcExt = transportWideCCExt;
    return this;
  }

  public boolean getRecord() {
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

  public boolean getLockRecord() {
    return lockRecord;
  }

  public VideoRoomCreateRequest lockRecord(boolean lockRecord) {
    this.lockRecord = lockRecord;
    return this;
  }

  public boolean getNotifyJoining() {
    return notifyJoining;
  }

  public VideoRoomCreateRequest notifyJoining(boolean notifyJoining) {
    this.notifyJoining = notifyJoining;
    return this;
  }

  public boolean getRequireE2ee() {
    return requireE2ee;
  }

  public VideoRoomCreateRequest requireE2ee(boolean requireE2ee) {
    this.requireE2ee = requireE2ee;
    return this;
  }

  public boolean isDummyPublisher() {
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
