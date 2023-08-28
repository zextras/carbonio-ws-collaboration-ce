// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PluginErrorResponse;

/**
 * This class represents a video room response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomResponse {

  public static final String CREATED   = "created";
  public static final String EDITED    = "edited";
  public static final String DESTROYED = "destroyed";
  public static final String ACK       = "ack";

  @JsonProperty("janus")
  private String              status;
  @JsonProperty("session_id")
  private String              connectionId;
  @JsonProperty("transaction")
  private String              transactionId;
  @JsonProperty("sender")
  private String              handleId;
  @JsonProperty("plugindata")
  private VideoRoomPluginData pluginData;

  private PluginErrorResponse error;

  public VideoRoomResponse() {
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

  public VideoRoomPluginData getPluginData() {
    return pluginData;
  }

  public PluginErrorResponse getError() {
    return error;
  }
}
