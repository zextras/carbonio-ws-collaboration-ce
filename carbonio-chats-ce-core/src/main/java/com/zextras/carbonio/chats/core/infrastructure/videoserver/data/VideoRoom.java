package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoRoom extends Room {

  public static final String NAME = "video_room";

  @JsonProperty
  private String       room;
  @JsonProperty
  private Boolean      permanent;
  @JsonProperty
  private String       description;
  @JsonProperty
  private String       secret;
  @JsonProperty
  private String       pin;
  @JsonProperty
  private Boolean      isPrivate;
  @JsonProperty
  private List<String> allowed;
  @JsonProperty
  private Boolean      requirePvtId;
  @JsonProperty
  private Short        publishers;
  @JsonProperty
  private Long         bitrate;
  @JsonProperty
  private Boolean      bitrateCap;
  @JsonProperty
  private Integer      firFreq;
  @JsonProperty
  private AudioCodec   audioCodec;
  @JsonProperty
  private VideoCodec   videoCodec;
  @JsonProperty
  private Short        vp9Profile;
  @JsonProperty
  private Short        h264Profile;
  @JsonProperty
  private Boolean      opusFEC;
  @JsonProperty
  private Boolean      videoSVC;
  @JsonProperty
  private Boolean      audioLevelExt;
  @JsonProperty
  private Boolean      audioLevelEvent;
  @JsonProperty
  private Short        audioLevelAverage;
  @JsonProperty
  private Boolean      videoOrientExt;
  @JsonProperty
  private Boolean      playoutDelayExt;
  @JsonProperty
  private Boolean      transportWideCCExt;
  @JsonProperty
  private Boolean      record;
  @JsonProperty
  private String       recordDir;
  @JsonProperty
  private Boolean      lockRecord;
  @JsonProperty
  private Boolean      notifyJoining;
  @JsonProperty
  private Boolean      requireE2ee;

  public VideoRoom() {
  }

  public VideoRoom(String room, Boolean permanent, String description, String secret, String pin, Boolean isPrivate,
    List<String> allowed, Boolean requirePvtId, Short publishers, Long bitrate, Boolean bitrateCap, Integer firFreq,
    AudioCodec audioCodec, VideoCodec videoCodec, Short vp9Profile, Short h264Profile, Boolean opusFEC,
    Boolean videoSVC,
    Boolean audioLevelExt, Boolean audioLevelEvent, Short audioLevelAverage, Boolean videoOrientExt,
    Boolean playoutDelayExt, Boolean transportWideCCExt, Boolean record, String recordDir, Boolean lockRecord,
    Boolean notifyJoining, Boolean requireE2ee) {
    this.room = room;
    this.permanent = permanent;
    this.description = description;
    this.secret = secret;
    this.pin = pin;
    this.isPrivate = isPrivate;
    this.allowed = allowed;
    this.requirePvtId = requirePvtId;
    this.publishers = publishers;
    this.bitrate = bitrate;
    this.bitrateCap = bitrateCap;
    this.firFreq = firFreq;
    this.audioCodec = audioCodec;
    this.videoCodec = videoCodec;
    this.vp9Profile = vp9Profile;
    this.h264Profile = h264Profile;
    this.opusFEC = opusFEC;
    this.videoSVC = videoSVC;
    this.audioLevelExt = audioLevelExt;
    this.audioLevelEvent = audioLevelEvent;
    this.audioLevelAverage = audioLevelAverage;
    this.videoOrientExt = videoOrientExt;
    this.playoutDelayExt = playoutDelayExt;
    this.transportWideCCExt = transportWideCCExt;
    this.record = record;
    this.recordDir = recordDir;
    this.lockRecord = lockRecord;
    this.notifyJoining = notifyJoining;
    this.requireE2ee = requireE2ee;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public Boolean getPermanent() {
    return permanent;
  }

  public void setPermanent(Boolean permanent) {
    this.permanent = permanent;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getPin() {
    return pin;
  }

  public void setPin(String pin) {
    this.pin = pin;
  }

  public Boolean getPrivate() {
    return isPrivate;
  }

  public void setPrivate(Boolean aPrivate) {
    isPrivate = aPrivate;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public void setAllowed(List<String> allowed) {
    this.allowed = allowed;
  }

  public Boolean getRequirePvtId() {
    return requirePvtId;
  }

  public void setRequirePvtId(Boolean requirePvtId) {
    this.requirePvtId = requirePvtId;
  }

  public Short getPublishers() {
    return publishers;
  }

  public void setPublishers(Short publishers) {
    this.publishers = publishers;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public void setBitrate(Long bitrate) {
    this.bitrate = bitrate;
  }

  public Boolean getBitrateCap() {
    return bitrateCap;
  }

  public void setBitrateCap(Boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
  }

  public Integer getFirFreq() {
    return firFreq;
  }

  public void setFirFreq(Integer firFreq) {
    this.firFreq = firFreq;
  }

  public AudioCodec getAudioCodec() {
    return audioCodec;
  }

  public void setAudioCodec(AudioCodec audioCodec) {
    this.audioCodec = audioCodec;
  }

  public VideoCodec getVideoCodec() {
    return videoCodec;
  }

  public void setVideoCodec(VideoCodec videoCodec) {
    this.videoCodec = videoCodec;
  }

  public Short getVp9Profile() {
    return vp9Profile;
  }

  public void setVp9Profile(Short vp9Profile) {
    this.vp9Profile = vp9Profile;
  }

  public Short getH264Profile() {
    return h264Profile;
  }

  public void setH264Profile(Short h264Profile) {
    this.h264Profile = h264Profile;
  }

  public Boolean getOpusFEC() {
    return opusFEC;
  }

  public void setOpusFEC(Boolean opusFEC) {
    this.opusFEC = opusFEC;
  }

  public Boolean getVideoSVC() {
    return videoSVC;
  }

  public void setVideoSVC(Boolean videoSVC) {
    this.videoSVC = videoSVC;
  }

  public Boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public void setAudioLevelExt(Boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
  }

  public Boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public void setAudioLevelEvent(Boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
  }

  public Short getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public void setAudioLevelAverage(Short audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
  }

  public Boolean getVideoOrientExt() {
    return videoOrientExt;
  }

  public void setVideoOrientExt(Boolean videoOrientExt) {
    this.videoOrientExt = videoOrientExt;
  }

  public Boolean getPlayoutDelayExt() {
    return playoutDelayExt;
  }

  public void setPlayoutDelayExt(Boolean playoutDelayExt) {
    this.playoutDelayExt = playoutDelayExt;
  }

  public Boolean getTransportWideCCExt() {
    return transportWideCCExt;
  }

  public void setTransportWideCCExt(Boolean transportWideCCExt) {
    this.transportWideCCExt = transportWideCCExt;
  }

  public Boolean getRecord() {
    return record;
  }

  public void setRecord(Boolean record) {
    this.record = record;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public void setRecordDir(String recordDir) {
    this.recordDir = recordDir;
  }

  public Boolean getLockRecord() {
    return lockRecord;
  }

  public void setLockRecord(Boolean lockRecord) {
    this.lockRecord = lockRecord;
  }

  public Boolean getNotifyJoining() {
    return notifyJoining;
  }

  public void setNotifyJoining(Boolean notifyJoining) {
    this.notifyJoining = notifyJoining;
  }

  public Boolean getRequireE2ee() {
    return requireE2ee;
  }

  public void setRequireE2ee(Boolean requireE2ee) {
    this.requireE2ee = requireE2ee;
  }
}
