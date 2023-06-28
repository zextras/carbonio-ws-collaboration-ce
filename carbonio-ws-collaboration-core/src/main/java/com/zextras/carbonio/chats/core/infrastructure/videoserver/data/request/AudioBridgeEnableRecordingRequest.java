// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to enable recording for a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeEnableRecordingRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeEnableRecordingRequest {

  public static final String ENABLE_RECORDING = "enable_recording";

  private String  request;
  private String  room;
  private String  secret;
  private boolean record;
  private String  recordFile;
  private String  recordDir;

  public static AudioBridgeEnableRecordingRequest create() {
    return new AudioBridgeEnableRecordingRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeEnableRecordingRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeEnableRecordingRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeEnableRecordingRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public boolean isRecord() {
    return record;
  }

  public AudioBridgeEnableRecordingRequest record(boolean record) {
    this.record = record;
    return this;
  }

  public String getRecordFile() {
    return recordFile;
  }

  public AudioBridgeEnableRecordingRequest recordFile(String recordFile) {
    this.recordFile = recordFile;
    return this;
  }

  public String getRecordDir() {
    return recordDir;
  }

  public AudioBridgeEnableRecordingRequest recordDir(String recordDir) {
    this.recordDir = recordDir;
    return this;
  }
}
