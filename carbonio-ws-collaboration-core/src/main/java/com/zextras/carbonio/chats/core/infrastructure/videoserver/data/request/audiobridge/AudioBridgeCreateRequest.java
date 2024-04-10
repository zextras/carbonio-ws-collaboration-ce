// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the audio bridge request to create a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeCreateRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeCreateRequest extends AudioBridgeRequest {

  public static final String CREATE = "create";
  public static final String ROOM_DEFAULT = "audio_";
  public static final String DESCRIPTION_DEFAULT = "audio_room_";
  public static final long AUDIO_ACTIVE_PACKETS_DEFAULT = 10L;
  public static final int AUDIO_LEVEL_AVERAGE_DEFAULT = 65;
  public static final long SAMPLING_RATE_DEFAULT = 16000L;

  private String request;
  private String room;
  private Boolean permanent;
  private String description;
  private Boolean isPrivate;
  private Long samplingRate;

  @JsonProperty("audiolevel_event")
  private Boolean audioLevelEvent;

  private Long audioActivePackets;
  private Integer audioLevelAverage;

  private Boolean record;

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

  public Boolean getPermanent() {
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

  public Boolean getIsPrivate() {
    return isPrivate;
  }

  public AudioBridgeCreateRequest isPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
    return this;
  }

  public Long getSamplingRate() {
    return samplingRate;
  }

  public AudioBridgeCreateRequest samplingRate(long samplingRate) {
    this.samplingRate = samplingRate;
    return this;
  }

  public Boolean getAudioLevelEvent() {
    return audioLevelEvent;
  }

  public AudioBridgeCreateRequest audioLevelEvent(boolean audioLevelEvent) {
    this.audioLevelEvent = audioLevelEvent;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public AudioBridgeCreateRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public AudioBridgeCreateRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public AudioBridgeCreateRequest record(boolean record) {
    this.record = record;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AudioBridgeCreateRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getPermanent(), that.getPermanent())
        && Objects.equals(getDescription(), that.getDescription())
        && Objects.equals(getIsPrivate(), that.getIsPrivate())
        && Objects.equals(getSamplingRate(), that.getSamplingRate())
        && Objects.equals(getAudioLevelEvent(), that.getAudioLevelEvent())
        && Objects.equals(getAudioActivePackets(), that.getAudioActivePackets())
        && Objects.equals(getAudioLevelAverage(), that.getAudioLevelAverage())
        && Objects.equals(getRecord(), that.getRecord());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getRequest(),
        getRoom(),
        getPermanent(),
        getDescription(),
        getIsPrivate(),
        getSamplingRate(),
        getAudioLevelEvent(),
        getAudioActivePackets(),
        getAudioLevelAverage(),
        getRecord());
  }
}
