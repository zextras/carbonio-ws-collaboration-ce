// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to edit a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeEditRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeEditRequest {

  public static final String EDIT = "edit";

  private String  request;
  private String  room;
  private String  secret;
  private String  newDescription;
  private String  newSecret;
  private String  newPin;
  private boolean newIsPrivate;
  private String  newRecordDir;
  private String  newMjrsDir;
  private boolean permanent;

  public static AudioBridgeEditRequest create() {
    return new AudioBridgeEditRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeEditRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeEditRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeEditRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getNewDescription() {
    return newDescription;
  }

  public AudioBridgeEditRequest newDescription(String newDescription) {
    this.newDescription = newDescription;
    return this;
  }

  public String getNewSecret() {
    return newSecret;
  }

  public AudioBridgeEditRequest newSecret(String newSecret) {
    this.newSecret = newSecret;
    return this;
  }

  public String getNewPin() {
    return newPin;
  }

  public AudioBridgeEditRequest newPin(String newPin) {
    this.newPin = newPin;
    return this;
  }

  public boolean isNewIsPrivate() {
    return newIsPrivate;
  }

  public AudioBridgeEditRequest newIsPrivate(boolean newIsPrivate) {
    this.newIsPrivate = newIsPrivate;
    return this;
  }

  public String getNewRecordDir() {
    return newRecordDir;
  }

  public AudioBridgeEditRequest newRecordDir(String newRecordDir) {
    this.newRecordDir = newRecordDir;
    return this;
  }

  public String getNewMjrsDir() {
    return newMjrsDir;
  }

  public AudioBridgeEditRequest newMjrsDir(String newMjrsDir) {
    this.newMjrsDir = newMjrsDir;
    return this;
  }

  public boolean isPermanent() {
    return permanent;
  }

  public AudioBridgeEditRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }
}
