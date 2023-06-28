// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to play or stop playing a file in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgePlayFileRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgePlayFileRequest {

  public static final String PLAY_FILE = "play_file";
  public static final String STOP_FILE = "stop_file";

  private String  request;
  private String  room;
  private String  secret;
  private String  group;
  private String  fileId;
  private String  filename;
  private boolean loop;

  public static AudioBridgePlayFileRequest create() {
    return new AudioBridgePlayFileRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgePlayFileRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgePlayFileRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgePlayFileRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getGroup() {
    return group;
  }

  public AudioBridgePlayFileRequest group(String group) {
    this.group = group;
    return this;
  }

  public String getFileId() {
    return fileId;
  }

  public AudioBridgePlayFileRequest fileId(String fileId) {
    this.fileId = fileId;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public AudioBridgePlayFileRequest filename(String filename) {
    this.filename = filename;
    return this;
  }

  public boolean isLoop() {
    return loop;
  }

  public AudioBridgePlayFileRequest loop(boolean loop) {
    this.loop = loop;
    return this;
  }
}
