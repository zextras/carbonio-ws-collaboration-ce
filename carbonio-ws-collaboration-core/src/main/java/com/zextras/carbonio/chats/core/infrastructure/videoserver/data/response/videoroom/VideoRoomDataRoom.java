// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the data room contained in the video room response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomDataRoom {

  private String       room;
  private String       description;
  private Boolean      pinRequired;
  private Boolean      isPrivate;
  private Integer      maxPublishers;
  private Long         bitrate;
  private Boolean      bitrateCap;
  private Integer      firFreq;
  @JsonProperty("require_pvtid")
  private Boolean      requirePvtId;
  private Boolean      requireE2ee;
  private Boolean      dummyPublisher;
  private Boolean      notifyJoining;
  private List<String> audioCodec;
  private List<String> videoCodec;
  private Boolean      opusFec;
  private Boolean      opusDtx;
  private Boolean      record;
  private String       recDir;
  private Boolean      lockRecord;
  private Integer      numParticipants;
  @JsonProperty("audiolevel_ext")
  private Boolean      audioLevelExt;
  @JsonProperty("audiolevel_event")
  private Boolean      audioLevelEvent;
  private Long         audioActivePackets;
  private Long         audioLevelAverage;
  @JsonProperty("videoorient_ext")
  private Boolean      videoOrientExt;
  @JsonProperty("playoutdelay_ext")
  private Boolean      playOutDelayExt;
  private Boolean      transportWideCcExt;

  public static VideoRoomDataRoom create() {
    return new VideoRoomDataRoom();
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomDataRoom room(String room) {
    this.room = room;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public VideoRoomDataRoom description(String description) {
    this.description = description;
    return this;
  }

  public Boolean getPinRequired() {
    return pinRequired;
  }

  public VideoRoomDataRoom pinRequired(boolean pinRequired) {
    this.pinRequired = pinRequired;
    return this;
  }

  public Boolean getPrivate() {
    return isPrivate;
  }

  public VideoRoomDataRoom setPrivate(boolean status) {
    isPrivate = status;
    return this;
  }

  public Integer getMaxPublishers() {
    return maxPublishers;
  }

  public VideoRoomDataRoom maxPublishers(int maxPublishers) {
    this.maxPublishers = maxPublishers;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public VideoRoomDataRoom bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Boolean getBitrateCap() {
    return bitrateCap;
  }

  public VideoRoomDataRoom bitrateCap(boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
    return this;
  }

  public Integer getFirFreq() {
    return firFreq;
  }

  public VideoRoomDataRoom firFreq(int firFreq) {
    this.firFreq = firFreq;
    return this;
  }

  public Boolean getRequirePvtId() {
    return requirePvtId;
  }

  public VideoRoomDataRoom requirePvtId(boolean requirePvtId) {
    this.requirePvtId = requirePvtId;
    return this;
  }

  public Boolean getRequireE2ee() {
    return requireE2ee;
  }

  public VideoRoomDataRoom requireE2ee(boolean requireE2ee) {
    this.requireE2ee = requireE2ee;
    return this;
  }

  public Boolean getDummyPublisher() {
    return dummyPublisher;
  }

  public VideoRoomDataRoom dummyPublisher(boolean dummyPublisher) {
    this.dummyPublisher = dummyPublisher;
    return this;
  }

  public Boolean getNotifyJoining() {
    return notifyJoining;
  }

  public VideoRoomDataRoom notifyJoining(boolean notifyJoining) {
    this.notifyJoining = notifyJoining;
    return this;
  }

  public List<String> getAudioCodec() {
    return audioCodec;
  }

  public VideoRoomDataRoom audioCodec(List<String> audioCodec) {
    this.audioCodec = audioCodec;
    return this;
  }

  public List<String> getVideoCodec() {
    return videoCodec;
  }

  public VideoRoomDataRoom videoCodec(List<String> videoCodec) {
    this.videoCodec = videoCodec;
    return this;
  }

  public Boolean getOpusFec() {
    return opusFec;
  }

  public VideoRoomDataRoom opusFec(boolean opusFec) {
    this.opusFec = opusFec;
    return this;
  }

  public Boolean getOpusDtx() {
    return opusDtx;
  }

  public VideoRoomDataRoom opusDtx(boolean opusDtx) {
    this.opusDtx = opusDtx;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public VideoRoomDataRoom record(boolean record) {
    this.record = record;
    return this;
  }

  public String getRecDir() {
    return recDir;
  }

  public VideoRoomDataRoom recDir(String recDir) {
    this.recDir = recDir;
    return this;
  }

  public Boolean getLockRecord() {
    return lockRecord;
  }

  public VideoRoomDataRoom lockRecord(boolean lockRecord) {
    this.lockRecord = lockRecord;
    return this;
  }

  public Integer getNumParticipants() {
    return numParticipants;
  }

  public VideoRoomDataRoom numParticipants(int numParticipants) {
    this.numParticipants = numParticipants;
    return this;
  }

  public Boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public VideoRoomDataRoom audioLevelExt(boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
    return this;
  }

  public Boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public VideoRoomDataRoom audioLevelEvent(boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public VideoRoomDataRoom audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Long getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomDataRoom audioLevelAverage(long audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Boolean getVideoOrientExt() {
    return videoOrientExt;
  }

  public VideoRoomDataRoom videoOrientExt(boolean videoOrientExt) {
    this.videoOrientExt = videoOrientExt;
    return this;
  }

  public Boolean getPlayOutDelayExt() {
    return playOutDelayExt;
  }

  public VideoRoomDataRoom playOutDelayExt(boolean playOutDelayExt) {
    this.playOutDelayExt = playOutDelayExt;
    return this;
  }

  public Boolean getTransportWideCcExt() {
    return transportWideCcExt;
  }

  public VideoRoomDataRoom transportWideCcExt(boolean transportWideCcExt) {
    this.transportWideCcExt = transportWideCcExt;
    return this;
  }
}
