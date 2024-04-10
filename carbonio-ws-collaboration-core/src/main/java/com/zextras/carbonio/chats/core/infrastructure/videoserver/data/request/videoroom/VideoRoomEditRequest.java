// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the video room request to edit a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomEditRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomEditRequest extends VideoRoomRequest {

  public static final String EDIT = "edit";

  private String request;
  private String room;

  private String newRecDir;

  public static VideoRoomEditRequest create() {
    return new VideoRoomEditRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomEditRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomEditRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getNewRecDir() {
    return newRecDir;
  }

  public VideoRoomEditRequest newRecDir(String newRecDir) {
    this.newRecDir = newRecDir;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomEditRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getNewRecDir(), that.getNewRecDir());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getRoom(), getNewRecDir());
  }
}
