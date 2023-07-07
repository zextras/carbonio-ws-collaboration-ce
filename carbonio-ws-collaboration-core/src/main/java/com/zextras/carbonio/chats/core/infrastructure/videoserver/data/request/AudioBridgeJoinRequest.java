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
 * This class represents the audio bridge request to join a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeJoinRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeJoinRequest extends AudioBridgeRequest {

  public static final String JOIN = "join";

  public static final String FILENAME_DEFAULT = "audio";

  private String       request;
  private String       room;
  private String       id;
  private String       group;
  private String       pin;
  private String       display;
  private String       token;
  private Boolean      muted;
  private List<String> codec;
  @JsonProperty("prebuffer")
  private Long         preBuffer;
  private Long         bitrate;
  private Integer      quality;
  private Integer      expectedLoss;
  private Integer      volume;
  private Integer      spatialPosition;
  private String       secret;
  private Integer      audioLevelAverage;
  private Long         audioActivePackets;
  private Boolean      record;
  private String       filename;

  public static AudioBridgeJoinRequest create() {
    return new AudioBridgeJoinRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeJoinRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeJoinRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public AudioBridgeJoinRequest id(String id) {
    this.id = id;
    return this;
  }

  public String getGroup() {
    return group;
  }

  public AudioBridgeJoinRequest group(String group) {
    this.group = group;
    return this;
  }

  public String getPin() {
    return pin;
  }

  public AudioBridgeJoinRequest pin(String pin) {
    this.pin = pin;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public AudioBridgeJoinRequest display(String display) {
    this.display = display;
    return this;
  }

  public String getToken() {
    return token;
  }

  public AudioBridgeJoinRequest token(String token) {
    this.token = token;
    return this;
  }

  public Boolean getMuted() {
    return muted;
  }

  public AudioBridgeJoinRequest muted(boolean muted) {
    this.muted = muted;
    return this;
  }

  public List<String> getCodec() {
    return codec;
  }

  public AudioBridgeJoinRequest codec(List<String> codec) {
    this.codec = codec;
    return this;
  }

  public Long getPreBuffer() {
    return preBuffer;
  }

  public AudioBridgeJoinRequest preBuffer(long preBuffer) {
    this.preBuffer = preBuffer;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public AudioBridgeJoinRequest bitrate(Long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Integer getQuality() {
    return quality;
  }

  public AudioBridgeJoinRequest quality(int quality) {
    this.quality = quality;
    return this;
  }

  public Integer getExpectedLoss() {
    return expectedLoss;
  }

  public AudioBridgeJoinRequest expectedLoss(int expectedLoss) {
    this.expectedLoss = expectedLoss;
    return this;
  }

  public Integer getVolume() {
    return volume;
  }

  public AudioBridgeJoinRequest volume(int volume) {
    this.volume = volume;
    return this;
  }

  public Integer getSpatialPosition() {
    return spatialPosition;
  }

  public AudioBridgeJoinRequest spatialPosition(int spatialPosition) {
    this.spatialPosition = spatialPosition;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeJoinRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public AudioBridgeJoinRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public AudioBridgeJoinRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public AudioBridgeJoinRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public AudioBridgeJoinRequest filename(String filename) {
    this.filename = filename;
    return this;
  }
}
