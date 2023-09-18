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
  private Boolean                          permanent;
  private Boolean                          exists;
  private List<String>                     allowed;
  @JsonProperty("list")
  private List<AudioBridgeDataRoom>        rooms;
  private List<AudioBridgeDataParticipant> participants;

  private String errorCode;
  private String error;

  public static AudioBridgeDataInfo create() {
    return new AudioBridgeDataInfo();
  }

  public String getAudioBridge() {
    return audioBridge;
  }

  public AudioBridgeDataInfo audioBridge(String audioBridge) {
    this.audioBridge = audioBridge;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeDataInfo room(String room) {
    this.room = room;
    return this;
  }

  public Boolean getPermanent() {
    return permanent;
  }

  public AudioBridgeDataInfo permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public Boolean getExists() {
    return exists;
  }

  public AudioBridgeDataInfo exists(boolean exists) {
    this.exists = exists;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public AudioBridgeDataInfo allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }

  public List<AudioBridgeDataRoom> getRooms() {
    return rooms;
  }

  public AudioBridgeDataInfo rooms(List<AudioBridgeDataRoom> rooms) {
    this.rooms = rooms;
    return this;
  }

  public List<AudioBridgeDataParticipant> getParticipants() {
    return participants;
  }

  public AudioBridgeDataInfo participants(List<AudioBridgeDataParticipant> participants) {
    this.participants = participants;
    return this;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public AudioBridgeDataInfo errorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  public String getError() {
    return error;
  }

  public AudioBridgeDataInfo error(String error) {
    this.error = error;
    return this;
  }
}
