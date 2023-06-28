// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to join a video room as subscriber.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomJoinSubscriberRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomJoinSubscriberRequest {

  public static final String JOIN       = "join";
  public static final String SUBSCRIBER = "subscriber";

  private String       request;
  private String       ptype;
  private String       room;
  private boolean      useMsid;
  private boolean      autoupdate;
  private String       privateId;
  private List<Stream> streams;

  public static VideoRoomJoinSubscriberRequest create() {
    return new VideoRoomJoinSubscriberRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomJoinSubscriberRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getPtype() {
    return ptype;
  }

  public VideoRoomJoinSubscriberRequest ptype(String ptype) {
    this.ptype = ptype;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomJoinSubscriberRequest room(String room) {
    this.room = room;
    return this;
  }

  public boolean isUseMsid() {
    return useMsid;
  }

  public VideoRoomJoinSubscriberRequest useMsid(boolean useMsid) {
    this.useMsid = useMsid;
    return this;
  }

  public boolean isAutoupdate() {
    return autoupdate;
  }

  public VideoRoomJoinSubscriberRequest autoupdate(boolean autoupdate) {
    this.autoupdate = autoupdate;
    return this;
  }

  public String getPrivateId() {
    return privateId;
  }

  public VideoRoomJoinSubscriberRequest privateId(String privateId) {
    this.privateId = privateId;
    return this;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomJoinSubscriberRequest streams(List<Stream> streams) {
    this.streams = streams;
    return this;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private class Stream {

    private String feed;
    private String mid;
    private String crossrefid;

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

    public String getCrossrefid() {
      return crossrefid;
    }

    public Stream crossrefid(String crossrefid) {
      this.crossrefid = crossrefid;
      return this;
    }
  }
}
