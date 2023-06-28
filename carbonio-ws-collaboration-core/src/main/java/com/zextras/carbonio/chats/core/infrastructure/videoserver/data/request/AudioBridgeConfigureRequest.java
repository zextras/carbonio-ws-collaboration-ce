// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to change media settings for a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeConfigureRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeConfigureRequest {

  public static final String CONFIGURE = "configure";

  private String  request;
  private boolean muted;
  private String  display;
  @JsonProperty("prebuffer")
  private long    preBuffer;
  private long    bitrate;
  private int     quality;
  private int     expectedLoss;
  private int     volume;
  private int     spatialPosition;
  private boolean record;
  private String  filename;
  private String  group;

  public static AudioBridgeConfigureRequest create() {
    return new AudioBridgeConfigureRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeConfigureRequest request(String request) {
    this.request = request;
    return this;
  }

  public boolean isMuted() {
    return muted;
  }

  public AudioBridgeConfigureRequest muted(boolean muted) {
    this.muted = muted;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public AudioBridgeConfigureRequest display(String display) {
    this.display = display;
    return this;
  }

  public long getPreBuffer() {
    return preBuffer;
  }

  public AudioBridgeConfigureRequest preBuffer(long preBuffer) {
    this.preBuffer = preBuffer;
    return this;
  }

  public long getBitrate() {
    return bitrate;
  }

  public AudioBridgeConfigureRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public int getQuality() {
    return quality;
  }

  public AudioBridgeConfigureRequest quality(int quality) {
    this.quality = quality;
    return this;
  }

  public int getExpectedLoss() {
    return expectedLoss;
  }

  public AudioBridgeConfigureRequest expectedLoss(int expectedLoss) {
    this.expectedLoss = expectedLoss;
    return this;
  }

  public int getVolume() {
    return volume;
  }

  public AudioBridgeConfigureRequest volume(int volume) {
    this.volume = volume;
    return this;
  }

  public int getSpatialPosition() {
    return spatialPosition;
  }

  public AudioBridgeConfigureRequest spatialPosition(int spatialPosition) {
    this.spatialPosition = spatialPosition;
    return this;
  }

  public boolean isRecord() {
    return record;
  }

  public AudioBridgeConfigureRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public AudioBridgeConfigureRequest filename(String filename) {
    this.filename = filename;
    return this;
  }

  public String getGroup() {
    return group;
  }

  public AudioBridgeConfigureRequest group(String group) {
    this.group = group;
    return this;
  }
}
