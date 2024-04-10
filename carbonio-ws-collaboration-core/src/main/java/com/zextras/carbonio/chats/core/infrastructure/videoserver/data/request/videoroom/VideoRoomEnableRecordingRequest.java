// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the video room request to enable recording on a room.
 *
 * @see <a
 *     href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomEnableRecordingRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomEnableRecordingRequest extends VideoRoomRequest {

  public static final String ENABLE_RECORDING = "enable_recording";

  private String request;
  private String room;
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

  public Boolean getRecord() {
    return record;
  }

  public VideoRoomEnableRecordingRequest record(boolean record) {
    this.record = record;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomEnableRecordingRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getRecord(), that.getRecord());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getRoom(), getRecord());
  }
}
