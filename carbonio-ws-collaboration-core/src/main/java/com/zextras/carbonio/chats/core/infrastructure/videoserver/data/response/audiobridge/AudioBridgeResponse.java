// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PluginErrorResponse;

/**
 * This class represents an audio bridge response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeResponse {

  public static final String CREATED   = "created";
  public static final String EDITED    = "edited";
  public static final String DESTROYED = "destroyed";
  public static final String ACK       = "ack";

  public static final String SUCCESS      = "success";
  public static final String PARTICIPANTS = "participants";

  @JsonProperty("janus")
  private String                status;
  @JsonProperty("session_id")
  private String                connectionId;
  @JsonProperty("transaction")
  private String                transactionId;
  @JsonProperty("sender")
  private String                handleId;
  @JsonProperty("plugindata")
  private AudioBridgePluginData pluginData;

  private PluginErrorResponse error;

  public static AudioBridgeResponse create() {
    return new AudioBridgeResponse();
  }

  public String getStatus() {
    return status;
  }

  public AudioBridgeResponse status(String status) {
    this.status = status;
    return this;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public AudioBridgeResponse connectionId(String connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public AudioBridgeResponse transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public String getHandleId() {
    return handleId;
  }

  public AudioBridgeResponse handleId(String handleId) {
    this.handleId = handleId;
    return this;
  }

  public AudioBridgePluginData getPluginData() {
    return pluginData;
  }

  public AudioBridgeResponse pluginData(AudioBridgePluginData pluginData) {
    this.pluginData = pluginData;
    return this;
  }

  public PluginErrorResponse getError() {
    return error;
  }

  public AudioBridgeResponse error(PluginErrorResponse error) {
    this.error = error;
    return this;
  }

  @JsonIgnore
  public String getAudioBridge() {
    return getPluginData().getDataInfo().getAudioBridge();
  }

  @JsonIgnore
  public String getRoom() {
    return getPluginData().getDataInfo().getRoom();
  }
}
