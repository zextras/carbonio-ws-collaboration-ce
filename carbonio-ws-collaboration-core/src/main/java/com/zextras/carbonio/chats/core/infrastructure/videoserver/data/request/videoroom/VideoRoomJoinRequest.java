// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import java.util.List;

/**
 * This class represents the video room request to join a video room as publisher or as subscriber.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomJoinRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomJoinRequest extends VideoRoomRequest {

  public static final String JOIN               = "join";
  public static final String JOIN_AND_CONFIGURE = "joinandconfigure";

  //publisher
  private String request;
  private String ptype;
  private String room;
  private String id;
  private String display;
  private String token;

  //subscriber
  private Boolean      useMsid;
  private Boolean      autoupdate;
  private String       privateId;
  private List<Stream> streams;

  public static VideoRoomJoinRequest create() {
    return new VideoRoomJoinRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomJoinRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getPtype() {
    return ptype;
  }

  public VideoRoomJoinRequest ptype(String ptype) {
    this.ptype = ptype;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomJoinRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public VideoRoomJoinRequest id(String id) {
    this.id = id;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public VideoRoomJoinRequest display(String display) {
    this.display = display;
    return this;
  }

  public String getToken() {
    return token;
  }

  public VideoRoomJoinRequest token(String token) {
    this.token = token;
    return this;
  }

  public Boolean isUseMsid() {
    return useMsid;
  }

  public VideoRoomJoinRequest useMsid(boolean useMsid) {
    this.useMsid = useMsid;
    return this;
  }

  public Boolean isAutoupdate() {
    return autoupdate;
  }

  public VideoRoomJoinRequest autoupdate(boolean autoupdate) {
    this.autoupdate = autoupdate;
    return this;
  }

  public String getPrivateId() {
    return privateId;
  }

  public VideoRoomJoinRequest privateId(String privateId) {
    this.privateId = privateId;
    return this;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomJoinRequest streams(List<Stream> streams) {
    this.streams = streams;
    return this;
  }
}
