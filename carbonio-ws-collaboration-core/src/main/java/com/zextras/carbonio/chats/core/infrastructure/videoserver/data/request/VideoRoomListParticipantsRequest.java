// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to list all participants in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomListParticipantsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomListParticipantsRequest {

  public static final String LIST_PARTICIPANTS = "listparticipants";

  private String request;
  private String room;

  public static VideoRoomListParticipantsRequest create() {
    return new VideoRoomListParticipantsRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomListParticipantsRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomListParticipantsRequest room(String room) {
    this.room = room;
    return this;
  }
}
