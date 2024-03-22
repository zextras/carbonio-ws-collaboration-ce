// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the video room request to publish a stream in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomPublishRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomPublishRequest extends VideoRoomRequest {

  public static final String PUBLISH = "publish";

  private String request;

  @JsonProperty("audiocodec")
  private String audioCodec;

  @JsonProperty("videocodec")
  private String videoCodec;

  private Long bitrate;
  private Boolean record;
  private String filename;
  private String display;
  private Integer audioLevelAverage;
  private Long audioActivePackets;
  private List<VideoRoomDescription> descriptions;

  public static VideoRoomPublishRequest create() {
    return new VideoRoomPublishRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomPublishRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getAudioCodec() {
    return audioCodec;
  }

  public VideoRoomPublishRequest audioCodec(String audioCodec) {
    this.audioCodec = audioCodec;
    return this;
  }

  public String getVideoCodec() {
    return videoCodec;
  }

  public VideoRoomPublishRequest videoCodec(String videoCodec) {
    this.videoCodec = videoCodec;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public VideoRoomPublishRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Boolean isRecord() {
    return record;
  }

  public VideoRoomPublishRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public VideoRoomPublishRequest filename(String filename) {
    this.filename = filename;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public VideoRoomPublishRequest display(String display) {
    this.display = display;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomPublishRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public VideoRoomPublishRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public List<VideoRoomDescription> getDescriptions() {
    return descriptions;
  }

  public VideoRoomPublishRequest descriptions(List<VideoRoomDescription> descriptions) {
    this.descriptions = descriptions;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomPublishRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getAudioCodec(), that.getAudioCodec())
        && Objects.equals(getVideoCodec(), that.getVideoCodec())
        && Objects.equals(getBitrate(), that.getBitrate())
        && Objects.equals(record, that.record)
        && Objects.equals(getFilename(), that.getFilename())
        && Objects.equals(getDisplay(), that.getDisplay())
        && Objects.equals(getAudioLevelAverage(), that.getAudioLevelAverage())
        && Objects.equals(getAudioActivePackets(), that.getAudioActivePackets())
        && Objects.equals(getDescriptions(), that.getDescriptions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getRequest(),
        getAudioCodec(),
        getVideoCodec(),
        getBitrate(),
        record,
        getFilename(),
        getDisplay(),
        getAudioLevelAverage(),
        getAudioActivePackets(),
        getDescriptions());
  }
}
