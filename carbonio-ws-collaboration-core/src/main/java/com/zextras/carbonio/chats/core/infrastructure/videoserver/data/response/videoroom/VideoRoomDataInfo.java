// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * This class represents the data info contained in the video room response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoRoomDataInfo {

  @JsonProperty("videoroom")
  private String                         videoRoom;
  private String                         room;
  private String                         permanent;
  private Boolean                        exists;
  private List<String>                   allowed;
  @JsonProperty("list")
  private List<VideoRoomDataRoom>        rooms;
  private List<VideoRoomDataParticipant> participants;

  private String errorCode;
  private String error;

  public VideoRoomDataInfo() {
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  public String getRoom() {
    return room;
  }

  public String getPermanent() {
    return permanent;
  }

  public Boolean getExists() {
    return exists;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public List<VideoRoomDataRoom> getRooms() {
    return rooms;
  }

  public List<VideoRoomDataParticipant> getParticipants() {
    return participants;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getError() {
    return error;
  }
}
