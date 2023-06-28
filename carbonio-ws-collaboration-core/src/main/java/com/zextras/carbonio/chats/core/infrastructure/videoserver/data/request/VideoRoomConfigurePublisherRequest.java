// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

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
public class VideoRoomConfigurePublisherRequest {

  public static final String CONFIGURE = "configure";

  private String            request;
  private long              bitrate;
  private boolean           keyframe;
  private boolean           record;
  private String            filename;
  private String            display;
  private long              audioActivePackets;
  private int               audioLevelAverage;
  private List<Stream>      streams;
  private List<Description> descriptions;

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

  public long getBitrate() {
    return bitrate;
  }

  public VideoRoomConfigurePublisherRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public boolean isKeyframe() {
    return keyframe;
  }

  public VideoRoomConfigurePublisherRequest keyframe(boolean keyframe) {
    this.keyframe = keyframe;
    return this;
  }

  public boolean isRecord() {
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

  public long getAudioActivePackets() {
    return audioActivePackets;
  }

  public VideoRoomConfigurePublisherRequest audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public int getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomConfigurePublisherRequest audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomConfigurePublisherRequest streams(List<Stream> streams) {
    this.streams = streams;
    return this;
  }

  public List<Description> getDescriptions() {
    return descriptions;
  }

  public VideoRoomConfigurePublisherRequest descriptions(List<Description> descriptions) {
    this.descriptions = descriptions;
    return this;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private class Stream {

    private String  mid;
    private String  keyFrame;
    private boolean send;
    private int     minDelay;
    private int     maxDelay;

    public Stream create() {
      return new Stream();
    }

    public String getMid() {
      return mid;
    }

    public Stream mid(String mid) {
      this.mid = mid;
      return this;
    }

    public String getKeyFrame() {
      return keyFrame;
    }

    public Stream keyFrame(String keyFrame) {
      this.keyFrame = keyFrame;
      return this;
    }

    public boolean isSend() {
      return send;
    }

    public Stream send(boolean send) {
      this.send = send;
      return this;
    }

    public int getMinDelay() {
      return minDelay;
    }

    public Stream minDelay(int minDelay) {
      this.minDelay = minDelay;
      return this;
    }

    public int getMaxDelay() {
      return maxDelay;
    }

    public Stream maxDelay(int maxDelay) {
      this.maxDelay = maxDelay;
      return this;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private class Description {

    private String mid;
    private String description;

    public Description create() {
      return new Description();
    }

    public String getMid() {
      return mid;
    }

    public Description mid(String mid) {
      this.mid = mid;
      return this;
    }

    public String getDescription() {
      return description;
    }

    public Description description(String description) {
      this.description = description;
      return this;
    }
  }
}
