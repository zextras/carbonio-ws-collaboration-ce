// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.Stream;
import java.util.List;

/**
 * This class represents the video room request to join a video room as subscriber.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomJoinSubscriberRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomJoinSubscriberRequest extends VideoRoomRequest {

  public static final String JOIN       = "join";
  public static final String SUBSCRIBER = "subscriber";

  private String       request;
  private String       ptype;
  private String       room;
  private Boolean      useMsid;
  private Boolean      autoupdate;
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

  public Boolean isUseMsid() {
    return useMsid;
  }

  public VideoRoomJoinSubscriberRequest useMsid(boolean useMsid) {
    this.useMsid = useMsid;
    return this;
  }

  public Boolean isAutoupdate() {
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
}
