package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room entity with all its fields.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoom</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoom extends Room {

  private String       room;
  private Boolean      permanent;
  private String       description;
  private String       secret;
  private String       pin;
  private Boolean      isPrivate;
  private List<String> allowed;
  private Boolean      requirePvtId;
  private Short        publishers;
  private Long         bitrate;
  private Boolean      bitrateCap;
  private Integer      firFreq;
  private List<String> audioCodec;
  private List<String> videoCodec;
  private Short        vp9Profile;
  private Short        h264Profile;
  private Boolean      opusFEC;
  private Boolean      videoSVC;
  private Boolean      audioLevelExt;
  private Boolean      audioLevelEvent;
  private Short        audioLevelAverage;
  private Boolean      videoOrientExt;
  private Boolean      playoutDelayExt;
  private Boolean      transportWideCCExt;
  private Boolean      record;
  private String       recordDir;
  private Boolean      lockRecord;
  private Boolean      notifyJoining;
  private Boolean      requireE2ee;

  public static VideoRoom create() {
    return new VideoRoom();
  }

  public String getRoom() {
    return room;
  }

  public VideoRoom room(String room) {
    this.room = room;
    return this;
  }

  public Boolean isPermanent() {
    return permanent;
  }

  public VideoRoom permanent(Boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public VideoRoom description(String description) {
    this.description = description;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public VideoRoom secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getPin() {
    return pin;
  }

  public VideoRoom pin(String pin) {
    this.pin = pin;
    return this;
  }

  public Boolean isPrivate() {
    return isPrivate;
  }

  public VideoRoom isPrivate(Boolean isPrivate) {
    this.isPrivate = isPrivate;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public VideoRoom allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }

  public Boolean getRequirePvtId() {
    return requirePvtId;
  }

  public VideoRoom requirePvtId(Boolean requirePvtId) {
    this.requirePvtId = requirePvtId;
    return this;
  }

  public Short getPublishers() {
    return publishers;
  }

  public VideoRoom publishers(Short publishers) {
    this.publishers = publishers;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public VideoRoom bitrate(Long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Boolean getBitrateCap() {
    return bitrateCap;
  }

  public VideoRoom bitrateCap(Boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
    return this;
  }

  public Integer getFirFreq() {
    return firFreq;
  }

  public VideoRoom firFreq(Integer firFreq) {
    this.firFreq = firFreq;
    return this;
  }

  public List<String> getAudioCodec() {
    return audioCodec;
  }

  public VideoRoom audioCodec(List<String> audioCodec) {
    this.audioCodec = audioCodec;
    return this;
  }

  public List<String> getVideoCodec() {
    return videoCodec;
  }

  public VideoRoom videoCodec(List<String> videoCodec) {
    this.videoCodec = videoCodec;
    return this;
  }

  public Short getVp9Profile() {
    return vp9Profile;
  }

  public VideoRoom vp9Profile(Short vp9Profile) {
    this.vp9Profile = vp9Profile;
    return this;
  }

  public Short getH264Profile() {
    return h264Profile;
  }

  public VideoRoom h264Profile(Short h264Profile) {
    this.h264Profile = h264Profile;
    return this;
  }

  public Boolean getOpusFEC() {
    return opusFEC;
  }

  public VideoRoom opusFEC(Boolean opusFEC) {
    this.opusFEC = opusFEC;
    return this;
  }

  public Boolean getVideoSVC() {
    return videoSVC;
  }

  public VideoRoom videoSVC(Boolean videoSVC) {
    this.videoSVC = videoSVC;
    return this;
  }

  public Boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public VideoRoom audioLevelExt(Boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
    return this;
  }

  public Boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public VideoRoom audioLevelEvent(Boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public Short getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoom audioLevelAverage(Short audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Boolean getVideoOrientExt() {
    return videoOrientExt;
  }

  public VideoRoom videoOrientExt(Boolean videoOrientExt) {
    this.videoOrientExt = videoOrientExt;
    return this;
  }

  public Boolean getPlayoutDelayExt() {
    return playoutDelayExt;
  }

  public VideoRoom playoutDelayExt(Boolean playoutDelayExt) {
    this.playoutDelayExt = playoutDelayExt;
    return this;
  }

  public Boolean getTransportWideCCExt() {
    return transportWideCCExt;
  }

  public VideoRoom transportWideCCExt(Boolean transportWideCCExt) {
    this.transportWideCCExt = transportWideCCExt;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public VideoRoom record(Boolean record) {
    this.record = record;
    return this;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public VideoRoom recordDir(String recordDir) {
    this.recordDir = recordDir;
    return this;
  }

  public Boolean getLockRecord() {
    return lockRecord;
  }

  public VideoRoom lockRecord(Boolean lockRecord) {
    this.lockRecord = lockRecord;
    return this;
  }

  public Boolean getNotifyJoining() {
    return notifyJoining;
  }

  public VideoRoom notifyJoining(Boolean notifyJoining) {
    this.notifyJoining = notifyJoining;
    return this;
  }

  public Boolean getRequireE2ee() {
    return requireE2ee;
  }

  public VideoRoom requireE2ee(Boolean requireE2ee) {
    this.requireE2ee = requireE2ee;
    return this;
  }
}
