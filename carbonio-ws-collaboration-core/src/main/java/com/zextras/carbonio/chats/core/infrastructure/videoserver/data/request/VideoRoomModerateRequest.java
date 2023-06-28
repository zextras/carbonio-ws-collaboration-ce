// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to moderate a video room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomModerateRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomModerateRequest {

  public static final String MODERATE = "moderate";

  private String  request;
  private String  secret;
  private String  room;
  private String  id;
  private String  mid;
  private boolean mute;

  public static VideoRoomModerateRequest create() {
    return new VideoRoomModerateRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomModerateRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public VideoRoomModerateRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomModerateRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public VideoRoomModerateRequest id(String id) {
    this.id = id;
    return this;
  }

  public String getMid() {
    return mid;
  }

  public VideoRoomModerateRequest mid(String mid) {
    this.mid = mid;
    return this;
  }

  public boolean isMute() {
    return mute;
  }

  public VideoRoomModerateRequest mute(boolean mute) {
    this.mute = mute;
    return this;
  }
}
