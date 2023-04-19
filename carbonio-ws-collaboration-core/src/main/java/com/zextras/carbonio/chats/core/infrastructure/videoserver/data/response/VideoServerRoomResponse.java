// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.Participant;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.Room;
import java.util.List;

/**
 * This class represents a successful room response provided by VideoServer when creating or editing a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeRoom</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoom</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerRoomResponse implements VideoServerPluginResponse {

  @JsonProperty("audioroom")
  private String            audioRoom;
  @JsonProperty("videoroom")
  private String            videoRoom;
  private String            room;
  private String            permanent;
  private boolean           exists;
  private List<String>      allowed;
  @JsonProperty("list")
  private List<Room>        rooms;
  private List<Participant> participants;

  public String getAudioRoom() {
    return audioRoom;
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  @Override
  public String getRoom() {
    return room;
  }

  public String getPermanent() {
    return permanent;
  }

  public boolean isExists() {
    return exists;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public List<Room> getRooms() {
    return rooms;
  }

  public List<Participant> getParticipants() {
    return participants;
  }

  @Override
  public boolean statusOK() {
    return true;
  }
}
