// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

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

  public AudioBridgeResponse() {
  }

  public String getStatus() {
    return status;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getHandleId() {
    return handleId;
  }

  public AudioBridgePluginData getPluginData() {
    return pluginData;
  }

  public PluginErrorResponse getError() {
    return error;
  }
}
