// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to configure publishers a video room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomConfigurePublisherRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomConfigurePublisherRequest extends VideoRoomRequest {

  public static final String CONFIGURE = "configure";

  private String                     request;
  private Long                       bitrate;
  private Boolean                    keyframe;
  private Boolean                    record;
  private String                     filename;
  private String                     display;
  private Long                       audioActivePackets;
  private Integer                    audioLevelAverage;
  private List<VideoRoomStream>      streams;
  private List<VideoRoomDescription> descriptions;

  public static VideoRoomConfigurePublisherRequest create() {
    return new VideoRoomConfigurePublisherRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomConfigurePublisherRequest request(String request) {
    this.request = request;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public VideoRoomConfigurePublisherRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Boolean isKeyframe() {
    return keyframe;
  }

  public VideoRoomConfigurePublisherRequest keyframe(boolean keyframe) {
    this.keyframe = keyframe;
    return this;
  }

  public Boolean isRecord() {
    return record;
  }

  public VideoRoomConfigurePublisherRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public VideoRoomConfigurePublisherRequest filename(String filename) {
    this.filename = filename;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public VideoRoomConfigurePublisherRequest display(String display) {
    this.display = display;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public VideoRoomConfigurePublisherRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomConfigurePublisherRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public List<VideoRoomStream> getStreams() {
    return streams;
  }

  public VideoRoomConfigurePublisherRequest streams(List<VideoRoomStream> streams) {
    this.streams = streams;
    return this;
  }

  public List<VideoRoomDescription> getDescriptions() {
    return descriptions;
  }

  public VideoRoomConfigurePublisherRequest descriptions(List<VideoRoomDescription> descriptions) {
    this.descriptions = descriptions;
    return this;
  }
}
