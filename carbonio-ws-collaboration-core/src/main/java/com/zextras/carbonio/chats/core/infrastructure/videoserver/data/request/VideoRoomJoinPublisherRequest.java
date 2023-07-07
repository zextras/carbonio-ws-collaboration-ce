// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to join a video room as publisher.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomJoinPublisherRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomJoinPublisherRequest extends VideoRoomRequest {

  public static final String JOIN               = "join";
  public static final String JOIN_AND_CONFIGURE = "joinandconfigure";
  public static final String PUBLISHER          = "publisher";

  private String request;
  private String ptype;
  private String room;
  private String id;
  private String display;
  private String token;

  public static VideoRoomJoinPublisherRequest create() {
    return new VideoRoomJoinPublisherRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomJoinPublisherRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getPtype() {
    return ptype;
  }

  public VideoRoomJoinPublisherRequest ptype(String ptype) {
    this.ptype = ptype;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomJoinPublisherRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public VideoRoomJoinPublisherRequest id(String id) {
    this.id = id;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public VideoRoomJoinPublisherRequest display(String display) {
    this.display = display;
    return this;
  }

  public String getToken() {
    return token;
  }

  public VideoRoomJoinPublisherRequest token(String token) {
    this.token = token;
    return this;
  }
}
