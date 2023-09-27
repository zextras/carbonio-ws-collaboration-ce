// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room stream you can set in video room requests.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomStream</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomStream {

  private String  mid;
  private String  keyFrame;
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

  public static VideoRoomStream create() {
    return new VideoRoomStream();
  }

  public String getMid() {
    return mid;
  }

  public VideoRoomStream mid(String mid) {
    this.mid = mid;
    return this;
  }

  public String getKeyFrame() {
    return keyFrame;
  }

  public VideoRoomStream keyFrame(String keyFrame) {
    this.keyFrame = keyFrame;
    return this;
  }

  public Boolean isSend() {
    return send;
  }

  public VideoRoomStream send(boolean send) {
    this.send = send;
    return this;
  }

  public Integer getSubstream() {
    return substream;
  }

  public VideoRoomStream substream(int substream) {
    this.substream = substream;
    return this;
  }

  public Integer getTemporal() {
    return temporal;
  }

  public VideoRoomStream temporal(int temporal) {
    this.temporal = temporal;
    return this;
  }

  public Long getFallback() {
    return fallback;
  }

  public VideoRoomStream fallback(long fallback) {
    this.fallback = fallback;
    return this;
  }

  public Integer getSpatialLayer() {
    return spatialLayer;
  }

  public VideoRoomStream spatialLayer(int spatialLayer) {
    this.spatialLayer = spatialLayer;
    return this;
  }

  public Integer getTemporalLayer() {
    return temporalLayer;
  }

  public VideoRoomStream temporalLayer(int temporalLayer) {
    this.temporalLayer = temporalLayer;
    return this;
  }

  public Integer getAudioLevelAverage() {
    return audioLevelAverage;
  }

  public VideoRoomStream audioLevelAverage(int audioLevelAverage) {
    this.audioLevelAverage = audioLevelAverage;
    return this;
  }

  public Long getAudioActivePackets() {
    return audioActivePackets;
  }

  public VideoRoomStream audioActivePackets(long audioActivePackets) {
    this.audioActivePackets = audioActivePackets;
    return this;
  }

  public Integer getMinDelay() {
    return minDelay;
  }

  public VideoRoomStream minDelay(int minDelay) {
    this.minDelay = minDelay;
    return this;
  }

  public Integer getMaxDelay() {
    return maxDelay;
  }

  public VideoRoomStream maxDelay(int maxDelay) {
    this.maxDelay = maxDelay;
    return this;
  }
}
