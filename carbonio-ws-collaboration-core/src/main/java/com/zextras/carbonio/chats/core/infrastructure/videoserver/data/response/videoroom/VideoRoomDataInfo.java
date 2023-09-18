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
  private Boolean                        permanent;
  private Boolean                        exists;
  private List<String>                   allowed;
  @JsonProperty("list")
  private List<VideoRoomDataRoom>        rooms;
  private List<VideoRoomDataParticipant> participants;

  private String errorCode;
  private String error;

  public static VideoRoomDataInfo create() {
    return new VideoRoomDataInfo();
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  public VideoRoomDataInfo videoRoom(String videoRoom) {
    this.videoRoom = videoRoom;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomDataInfo room(String room) {
    this.room = room;
    return this;
  }

  public Boolean getPermanent() {
    return permanent;
  }

  public VideoRoomDataInfo permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public Boolean getExists() {
    return exists;
  }

  public VideoRoomDataInfo exists(boolean exists) {
    this.exists = exists;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public VideoRoomDataInfo allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }

  public List<VideoRoomDataRoom> getRooms() {
    return rooms;
  }

  public VideoRoomDataInfo rooms(List<VideoRoomDataRoom> rooms) {
    this.rooms = rooms;
    return this;
  }

  public List<VideoRoomDataParticipant> getParticipants() {
    return participants;
  }

  public VideoRoomDataInfo participants(List<VideoRoomDataParticipant> participants) {
    this.participants = participants;
    return this;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public VideoRoomDataInfo errorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  public String getError() {
    return error;
  }

  public VideoRoomDataInfo error(String error) {
    this.error = error;
    return this;
  }
}
