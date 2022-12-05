package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioBridgeRoom extends Room {

  public static final String NAME = "audio_bridge_room";

  @JsonProperty
  private String  room;
  @JsonProperty
  private Boolean isPermanent;
  @JsonProperty
  private String  description;
  @JsonProperty
  private String  secret;
  @JsonProperty
  private String  pin;
  @JsonProperty
  private Boolean isPrivate;
  @JsonProperty
  private String  allowed;
  @JsonProperty
  private Long    samplingRate;
  @JsonProperty
  private Boolean audioLevelExt;
  @JsonProperty
  private Boolean audioLevelEvent;
  @JsonProperty
  private Long    audioActivePackets;
  @JsonProperty
  private Short   audioLevelAverage;
  @JsonProperty
  private Short   defaultPreBuffering;
  @JsonProperty
  private Boolean hasToRecord;
  @JsonProperty
  private String  recordFile;
  @JsonProperty
  private String  recordDir;
  @JsonProperty
  private String  mjrsDir;

  public AudioBridgeRoom() {
  }

  public AudioBridgeRoom(String room, Boolean isPermanent, String description, String secret, String pin,
    Boolean isPrivate, String allowed, Long samplingRate, Boolean audioLevelExt, Boolean audioLevelEvent,
    Long audioActivePackets, Short audioLevelAverage, Short defaultPreBuffering, Boolean hasToRecord, String recordFile,
    String recordDir, String mjrsDir) {
    this.room = room;
    this.isPermanent = isPermanent;
    this.description = description;
    this.secret = secret;
    this.pin = pin;
    this.isPrivate = isPrivate;
    this.allowed = allowed;
    this.samplingRate = samplingRate;
    this.audioLevelExt = audioLevelExt;
    this.audioLevelEvent = audioLevelEvent;
    this.audioActivePackets = audioActivePackets;
    this.audioLevelAverage = audioLevelAverage;
    this.defaultPreBuffering = defaultPreBuffering;
    this.hasToRecord = hasToRecord;
    this.recordFile = recordFile;
    this.recordDir = recordDir;
    this.mjrsDir = mjrsDir;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public Boolean getPermanent() {
    return isPermanent;
  }

  public void setPermanent(Boolean permanent) {
    isPermanent = permanent;
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

  public String getAllowed() {
    return allowed;
  }

  public void setAllowed(String allowed) {
    this.allowed = allowed;
  }

  public Long getSamplingRate() {
    return samplingRate;
  }

  public void setSamplingRate(Long samplingRate) {
    this.samplingRate = samplingRate;
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

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public void setAudioActivePackets(Long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
  }

  public Short getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public void setAudioLevelAverage(Short audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
  }

  public Short getDefaultPreBuffering() {
    return defaultPreBuffering;
  }

  public void setDefaultPreBuffering(Short defaultPreBuffering) {
    this.defaultPreBuffering = defaultPreBuffering;
  }

  public Boolean getHasToRecord() {
    return hasToRecord;
  }

  public void setHasToRecord(Boolean hasToRecord) {
    this.hasToRecord = hasToRecord;
  }

  public String getRecordFile() {
    return recordFile;
  }

  public void setRecordFile(String recordFile) {
    this.recordFile = recordFile;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public void setRecordDir(String recordDir) {
    this.recordDir = recordDir;
  }

  public String getMjrsDir() {
    return mjrsDir;
  }

  public void setMjrsDir(String mjrsDir) {
    this.mjrsDir = mjrsDir;
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
