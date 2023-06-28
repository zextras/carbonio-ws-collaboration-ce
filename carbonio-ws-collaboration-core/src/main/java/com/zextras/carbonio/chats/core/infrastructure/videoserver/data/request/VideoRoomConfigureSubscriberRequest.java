// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to configure subscribers in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomSubscriberRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomConfigureSubscriberRequest {

  public static final String CONFIGURE = "configure";

  private String       request;
  private List<Stream> streams;
  private boolean      restart;

  public static VideoRoomConfigureSubscriberRequest create() {
    return new VideoRoomConfigureSubscriberRequest();
  }

  public String getRequest() {
    return request;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomConfigureSubscriberRequest streams(List<Stream> streams) {
    this.streams = streams;
    return this;
  }

  public boolean isRestart() {
    return restart;
  }

  public VideoRoomConfigureSubscriberRequest restart(boolean restart) {
    this.restart = restart;
    return this;
  }

  public VideoRoomConfigureSubscriberRequest request(String request) {
    this.request = request;
    return this;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private class Stream {

    private String  mid;
    private boolean send;
    private int     substream;
    private int     temporal;
    private long    fallback;
    private int     spatialLayer;
    private int     temporalLayer;
    private int     audioLevelAverage;
    private long    audioActivePackets;
    private int     minDelay;
    private int     maxDelay;

    public String getMid() {
      return mid;
    }

    public Stream mid(String mid) {
      this.mid = mid;
      return this;
    }

    public boolean isSend() {
      return send;
    }

    public Stream send(boolean send) {
      this.send = send;
      return this;
    }

    public int getSubstream() {
      return substream;
    }

    public Stream substream(int substream) {
      this.substream = substream;
      return this;
    }

    public int getTemporal() {
      return temporal;
    }

    public Stream temporal(int temporal) {
      this.temporal = temporal;
      return this;
    }

    public long getFallback() {
      return fallback;
    }

    public Stream fallback(long fallback) {
      this.fallback = fallback;
      return this;
    }

    public int getSpatialLayer() {
      return spatialLayer;
    }

    public Stream spatialLayer(int spatialLayer) {
      this.spatialLayer = spatialLayer;
      return this;
    }

    public int getTemporalLayer() {
      return temporalLayer;
    }

    public Stream temporalLayer(int temporalLayer) {
      this.temporalLayer = temporalLayer;
      return this;
    }

    public int getAudioLevelAverage() {
      return audioLevelAverage;
    }

    public Stream audioLevelAverage(int audioLevelAverage) {
      this.audioLevelAverage = audioLevelAverage;
      return this;
    }

    public long getAudioActivePackets() {
      return audioActivePackets;
    }

    public Stream audioActivePackets(long audioActivePackets) {
      this.audioActivePackets = audioActivePackets;
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
}
