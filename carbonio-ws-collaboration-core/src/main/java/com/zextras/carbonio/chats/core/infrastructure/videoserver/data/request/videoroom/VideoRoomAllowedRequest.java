// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to change ACL for a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomAllowedRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomAllowedRequest extends VideoRoomRequest {

  public static final String ALLOWED = "allowed";
  public static final String ENABLE  = "enable";
  public static final String DISABLE = "disable";
  public static final String ADD     = "add";
  public static final String REMOVE  = "remove";

  private String       request;
  private String       secret;
  private String       action;
  private String       room;
  private List<String> allowed;

  public static VideoRoomAllowedRequest create() {
    return new VideoRoomAllowedRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomAllowedRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public VideoRoomAllowedRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getAction() {
    return action;
  }

  public VideoRoomAllowedRequest action(String action) {
    this.action = action;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomAllowedRequest room(String room) {
    this.room = room;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public VideoRoomAllowedRequest allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }
}
