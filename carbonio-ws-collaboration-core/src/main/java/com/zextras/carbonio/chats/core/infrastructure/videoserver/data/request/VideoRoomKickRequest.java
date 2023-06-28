// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to kick a session from the video room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomKickRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomKickRequest {

  public static final String KICK = "kick";

  private String request;
  private String secret;
  private String room;
  private String id;

  public static VideoRoomKickRequest create() {
    return new VideoRoomKickRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomKickRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public VideoRoomKickRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomKickRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public VideoRoomKickRequest id(String id) {
    this.id = id;
    return this;
  }
}
