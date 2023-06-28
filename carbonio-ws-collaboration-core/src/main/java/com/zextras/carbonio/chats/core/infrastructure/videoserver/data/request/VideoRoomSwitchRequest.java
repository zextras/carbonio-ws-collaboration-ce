// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to switch any of the subscription streams to a different publisher in a
 * room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomSwitchRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomSwitchRequest {

  public static final String SWITCH = "switch";

  private String       request;
  private List<Stream> streams;

  public static VideoRoomSwitchRequest create() {
    return new VideoRoomSwitchRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomSwitchRequest request(String request) {
    this.request = request;
    return this;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomSwitchRequest streams(List<Stream> subscriptions) {
    this.streams = subscriptions;
    return this;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private class Stream {

    private String feed;
    private String mid;
    private String subMid;

    public Stream create() {
      return new Stream();
    }

    public String getFeed() {
      return feed;
    }

    public Stream feed(String feed) {
      this.feed = feed;
      return this;
    }

    public String getMid() {
      return mid;
    }

    public Stream mid(String mid) {
      this.mid = mid;
      return this;
    }

    public String getSubMid() {
      return subMid;
    }

    public Stream subMid(String subMid) {
      this.subMid = subMid;
      return this;
    }
  }
}
