// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to edit a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomEditRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomEditRequest extends VideoRoomRequest {

  public static final String EDIT = "edit";

  private String  request;
  private String  room;
  private String  secret;
  private String  newDescription;
  private String  newPin;
  private Boolean newIsPrivate;
  @JsonProperty("new_require_pvtid")
  private Boolean newRequirePvtId;
  private Long    newBitrate;
  private Integer newFirFreq;
  private Integer newPublishers;
  private String  newRecDir;
  private Boolean permanent;

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

  public String getSecret() {
    return secret;
  }

  public VideoRoomEditRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getNewDescription() {
    return newDescription;
  }

  public VideoRoomEditRequest newDescription(String newDescription) {
    this.newDescription = newDescription;
    return this;
  }

  public String getNewPin() {
    return newPin;
  }

  public VideoRoomEditRequest newPin(String newPin) {
    this.newPin = newPin;
    return this;
  }

  public Boolean isNewIsPrivate() {
    return newIsPrivate;
  }

  public VideoRoomEditRequest newIsPrivate(boolean newIsPrivate) {
    this.newIsPrivate = newIsPrivate;
    return this;
  }

  public Boolean isNewRequirePvtId() {
    return newRequirePvtId;
  }

  public VideoRoomEditRequest newRequirePvtId(boolean newRequirePvtId) {
    this.newRequirePvtId = newRequirePvtId;
    return this;
  }

  public Long getNewBitrate() {
    return newBitrate;
  }

  public VideoRoomEditRequest newBitrate(long newBitrate) {
    this.newBitrate = newBitrate;
    return this;
  }

  public Integer getNewFirFreq() {
    return newFirFreq;
  }

  public VideoRoomEditRequest newFirFreq(int newFirFreq) {
    this.newFirFreq = newFirFreq;
    return this;
  }

  public Integer getNewPublishers() {
    return newPublishers;
  }

  public VideoRoomEditRequest newPublishers(int newPublishers) {
    this.newPublishers = newPublishers;
    return this;
  }

  public String getNewRecDir() {
    return newRecDir;
  }

  public VideoRoomEditRequest newRecDir(String newRecDir) {
    this.newRecDir = newRecDir;
    return this;
  }

  public Boolean isPermanent() {
    return permanent;
  }

  public VideoRoomEditRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }
}