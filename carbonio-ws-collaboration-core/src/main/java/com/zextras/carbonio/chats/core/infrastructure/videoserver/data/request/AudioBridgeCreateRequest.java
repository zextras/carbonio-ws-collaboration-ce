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
 * This class represents the audio bridge request to create a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeCreateRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeCreateRequest {

  public static final String CREATE                       = "create";
  public static final String ROOM_DEFAULT                 = "audio_";
  public static final String DESCRIPTION_DEFAULT          = "audio_room_";
  public static final long   AUDIO_ACTIVE_PACKETS_DEFAULT = 10L;
  public static final int    AUDIO_LEVEL_AVERAGE_DEFAULT  = 55;
  public static final long   SAMPLING_RATE_DEFAULT        = 16000L;

  private String       request;
  private String       room;
  private boolean      permanent;
  private String       description;
  private String       secret;
  private String       pin;
  private boolean      isPrivate;
  private List<String> allowed;
  private long         samplingRate;
  private boolean      spatialAudio;
  @JsonProperty("audiolevel_ext")
  private boolean      audioLevelExt;
  @JsonProperty("audiolevel_event")
  private boolean      audioLevelEvent;
  private long         audioActivePackets;
  private int          audioLevelAverage;
  private long         defaultPreBuffering;
  @JsonProperty("default_expectedloss")
  private int          defaultExpectedLoss;
  private long         defaultBitrate;
  private boolean      record;
  private String       recordFile;
  private String       recordDir;
  private boolean      mjrs;
  private String       mjrsDir;
  private boolean      allowRtpParticipants;
  private List<String> groups;

  public static AudioBridgeCreateRequest create() {
    return new AudioBridgeCreateRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeCreateRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeCreateRequest room(String room) {
    this.room = room;
    return this;
  }

  public boolean isPermanent() {
    return permanent;
  }

  public AudioBridgeCreateRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public AudioBridgeCreateRequest description(String description) {
    this.description = description;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeCreateRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getPin() {
    return pin;
  }

  public AudioBridgeCreateRequest pin(String pin) {
    this.pin = pin;
    return this;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public AudioBridgeCreateRequest isPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public AudioBridgeCreateRequest allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }

  public long getSamplingRate() {
    return samplingRate;
  }

  public AudioBridgeCreateRequest samplingRate(long samplingRate) {
    this.samplingRate = samplingRate;
    return this;
  }

  public boolean isSpatialAudio() {
    return spatialAudio;
  }

  public AudioBridgeCreateRequest spatialAudio(boolean spatialAudio) {
    this.spatialAudio = spatialAudio;
    return this;
  }

  public boolean getAudioLevelExt() {
    return audioLevelExt;
  }

  public AudioBridgeCreateRequest audioLevelExt(boolean audioLevelExt) {
    this.audioLevelExt = audioLevelExt;
    return this;
  }

  public boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public AudioBridgeCreateRequest audioLevelEvent(boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public long getAudioActivePackets() {
    return audioActivePackets;
  }

  public AudioBridgeCreateRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public int getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public AudioBridgeCreateRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public long getDefaultPreBuffering() {
    return defaultPreBuffering;
  }

  public AudioBridgeCreateRequest defaultPreBuffering(long defaultPreBuffering) {
    this.defaultPreBuffering = defaultPreBuffering;
    return this;
  }

  public int getDefaultExpectedLoss() {
    return defaultExpectedLoss;
  }

  public AudioBridgeCreateRequest defaultExpectedLoss(int defaultExpectedLoss) {
    this.defaultExpectedLoss = defaultExpectedLoss;
    return this;
  }

  public long getDefaultBitrate() {
    return defaultBitrate;
  }

  public AudioBridgeCreateRequest defaultBitrate(long defaultBitrate) {
    this.defaultBitrate = defaultBitrate;
    return this;
  }

  public boolean isRecord() {
    return record;
  }

  public AudioBridgeCreateRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getRecordFile() {
    return recordFile;
  }

  public AudioBridgeCreateRequest recordFile(String recordFile) {
    this.recordFile = recordFile;
    return this;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public AudioBridgeCreateRequest recordDir(String recordDir) {
    this.recordDir = recordDir;
    return this;
  }

  public boolean isMjrs() {
    return mjrs;
  }

  public AudioBridgeCreateRequest mjrs(boolean mjrs) {
    this.mjrs = mjrs;
    return this;
  }

  public String getMjrsDir() {
    return mjrsDir;
  }

  public AudioBridgeCreateRequest mjrsDir(String mjrsDir) {
    this.mjrsDir = mjrsDir;
    return this;
  }

  public boolean isAllowRtpParticipants() {
    return allowRtpParticipants;
  }

  public AudioBridgeCreateRequest allowRtpParticipants(boolean allowRtpParticipants) {
    this.allowRtpParticipants = allowRtpParticipants;
    return this;
  }

  public List<String> getGroups() {
    return groups;
  }

  public AudioBridgeCreateRequest groups(List<String> groups) {
    this.groups = groups;
    return this;
  }
}
