// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * This class represents the data info contained in the audio bridge response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioBridgeDataInfo {

  @JsonProperty("audiobridge")
  private String                           audioBridge;
  private String                           room;
  private String                           permanent;
  private Boolean                          exists;
  private List<String>                     allowed;
  @JsonProperty("list")
  private List<AudioBridgeDataRoom>        rooms;
  private List<AudioBridgeDataParticipant> participants;

  private String errorCode;
  private String error;

  public AudioBridgeDataInfo() {
  }

  public String getAudioBridge() {
    return audioBridge;
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

  public List<AudioBridgeDataRoom> getRooms() {
    return rooms;
  }

  public List<AudioBridgeDataParticipant> getParticipants() {
    return participants;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getError() {
    return error;
  }
}
