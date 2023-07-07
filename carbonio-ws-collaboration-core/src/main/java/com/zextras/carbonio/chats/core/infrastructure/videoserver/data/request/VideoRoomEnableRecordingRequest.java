// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to enable recording on a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomEnableRecordingRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomEnableRecordingRequest extends VideoRoomRequest {

  public static final String ENABLE_RECORDING = "enable_recording";

  private String  request;
  private String  room;
  private String  secret;
  private Boolean record;

  public static VideoRoomEnableRecordingRequest create() {
    return new VideoRoomEnableRecordingRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomEnableRecordingRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomEnableRecordingRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public VideoRoomEnableRecordingRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public Boolean isRecord() {
    return record;
  }

  public VideoRoomEnableRecordingRequest record(boolean record) {
    this.record = record;
    return this;
  }
}
