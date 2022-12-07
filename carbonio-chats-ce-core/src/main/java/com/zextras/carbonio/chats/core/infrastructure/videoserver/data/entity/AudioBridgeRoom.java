package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AudioBridgeRoom extends Room {

  private String  room;
  private Boolean permanent;
  private String  description;
  private String  secret;
  private String  pin;
  private Boolean isPrivate;
  private String  allowed;
  private Long    samplingRate;
  private Boolean audioLevelExt;
  private Boolean audioLevelEvent;
  private Long    audioActivePackets;
  private Short   audioLevelAverage;
  private Short   defaultPreBuffering;
  private Boolean hasToRecord;
  private String  recordFile;
  private String  recordDir;
  private String  mjrsDir;

  public AudioBridgeRoom() {
  }

  public AudioBridgeRoom(String room, Boolean isPermanent, String description, String secret, String pin,
    Boolean isPrivate, String allowed, Long samplingRate, Boolean audioLevelExt, Boolean audioLevelEvent,
    Long audioActivePackets, Short audioLevelAverage, Short defaultPreBuffering, Boolean hasToRecord, String recordFile,
    String recordDir, String mjrsDir) {
    this.room = room;
    this.permanent = isPermanent;
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

  public static AudioBridgeRoom create() {
    return new AudioBridgeRoom();
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeRoom room(String room) {
    this.room = room;
    return this;
  }

  public Boolean isPermanent() {
    return permanent;
  }

  public AudioBridgeRoom permanent(Boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public AudioBridgeRoom description(String description) {
    this.description = description;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeRoom secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getPin() {
    return pin;
  }

  public AudioBridgeRoom pin(String pin) {
    this.pin = pin;
    return this;
  }

  public Boolean isPrivate() {
    return isPrivate;
  }

  public AudioBridgeRoom isPrivate(Boolean isPrivate) {
    this.isPrivate = isPrivate;
    return this;
  }

  public String getAllowed() {
    return allowed;
  }

  public AudioBridgeRoom allowed(String allowed) {
    this.allowed = allowed;
    return this;
  }

  public Long getSamplingRate() {
    return samplingRate;
  }

  public AudioBridgeRoom samplingRate(Long samplingRate) {
    this.samplingRate = samplingRate;
    return this;
  }

  public Boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public AudioBridgeRoom audioLevelExt(Boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
    return this;
  }

  public Boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public AudioBridgeRoom audioLevelEvent(Boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public AudioBridgeRoom audioActivePackets(Long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Short getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public AudioBridgeRoom audioLevelAverage(Short audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Short getDefaultPreBuffering() {
    return defaultPreBuffering;
  }

  public AudioBridgeRoom defaultPreBuffering(Short defaultPreBuffering) {
    this.defaultPreBuffering = defaultPreBuffering;
    return this;
  }

  public Boolean hasToRecord() {
    return hasToRecord;
  }

  public AudioBridgeRoom hasToRecord(Boolean hasToRecord) {
    this.hasToRecord = hasToRecord;
    return this;
  }

  public String getRecordFile() {
    return recordFile;
  }

  public AudioBridgeRoom recordFile(String recordFile) {
    this.recordFile = recordFile;
    return this;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public AudioBridgeRoom recordDir(String recordDir) {
    this.recordDir = recordDir;
    return this;
  }

  public String getMjrsDir() {
    return mjrsDir;
  }

  public AudioBridgeRoom mjrsDir(String mjrsDir) {
    this.mjrsDir = mjrsDir;
    return this;
  }
}
