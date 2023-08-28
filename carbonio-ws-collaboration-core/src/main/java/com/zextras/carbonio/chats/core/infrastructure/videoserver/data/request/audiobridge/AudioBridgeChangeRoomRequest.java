// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to change a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeChangeRoomRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeChangeRoomRequest extends AudioBridgeRequest {

  public static final String CHANGE_ROOM = "changeroom";

  private String  request;
  private String  room;
  private String  id;
  private String  group;
  private String  display;
  private String  token;
  private Boolean muted;
  private Long    bitrate;
  private Integer quality;
  private Integer expectedLoss;

  public static AudioBridgeChangeRoomRequest create() {
    return new AudioBridgeChangeRoomRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeChangeRoomRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeChangeRoomRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public AudioBridgeChangeRoomRequest id(String id) {
    this.id = id;
    return this;
  }

  public String getGroup() {
    return group;
  }

  public AudioBridgeChangeRoomRequest group(String group) {
    this.group = group;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public AudioBridgeChangeRoomRequest display(String display) {
    this.display = display;
    return this;
  }

  public String getToken() {
    return token;
  }

  public AudioBridgeChangeRoomRequest token(String token) {
    this.token = token;
    return this;
  }

  public Boolean isMuted() {
    return muted;
  }

  public AudioBridgeChangeRoomRequest muted(boolean muted) {
    this.muted = muted;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public AudioBridgeChangeRoomRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Integer getQuality() {
    return quality;
  }

  public AudioBridgeChangeRoomRequest quality(int quality) {
    this.quality = quality;
    return this;
  }

  public Integer getExpectedLoss() {
    return expectedLoss;
  }

  public AudioBridgeChangeRoomRequest expectedLoss(int expectedLoss) {
    this.expectedLoss = expectedLoss;
    return this;
  }
}
