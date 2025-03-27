// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PluginErrorResponse;
import java.util.Objects;
import java.util.Optional;

/**
 * This class represents a video room response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomResponse {

  public static final String CREATED = "created";
  public static final String EDITED = "edited";
  public static final String DESTROYED = "destroyed";
  public static final String ACK = "ack";

  @JsonProperty("janus")
  private String status;

  @JsonProperty("session_id")
  private String connectionId;

  @JsonProperty("transaction")
  private String transactionId;

  @JsonProperty("sender")
  private String handleId;

  @JsonProperty("plugindata")
  private VideoRoomPluginData pluginData;

  private PluginErrorResponse error;

  public static VideoRoomResponse create() {
    return new VideoRoomResponse();
  }

  public String getStatus() {
    return status;
  }

  public VideoRoomResponse status(String status) {
    this.status = status;
    return this;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public VideoRoomResponse connectionId(String connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public VideoRoomResponse transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public String getHandleId() {
    return handleId;
  }

  public VideoRoomResponse handleId(String handleId) {
    this.handleId = handleId;
    return this;
  }

  public VideoRoomPluginData getPluginData() {
    return pluginData;
  }

  public VideoRoomResponse pluginData(VideoRoomPluginData pluginData) {
    this.pluginData = pluginData;
    return this;
  }

  public PluginErrorResponse getError() {
    return error;
  }

  public VideoRoomResponse error(PluginErrorResponse error) {
    this.error = error;
    return this;
  }

  @JsonIgnore
  public String getVideoRoom() {
    return Optional.ofNullable(getPluginData())
        .map(VideoRoomPluginData::getDataInfo)
        .map(VideoRoomDataInfo::getVideoRoom)
        .orElse(null);
  }

  @JsonIgnore
  public String getRoom() {
    return Optional.ofNullable(getPluginData())
        .map(VideoRoomPluginData::getDataInfo)
        .map(VideoRoomDataInfo::getRoom)
        .orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VideoRoomResponse)) {
      return false;
    }
    VideoRoomResponse that = (VideoRoomResponse) o;
    return Objects.equals(getStatus(), that.getStatus())
        && Objects.equals(getConnectionId(), that.getConnectionId())
        && Objects.equals(getTransactionId(), that.getTransactionId())
        && Objects.equals(getHandleId(), that.getHandleId())
        && Objects.equals(getPluginData(), that.getPluginData())
        && Objects.equals(getError(), that.getError());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getStatus(),
        getConnectionId(),
        getTransactionId(),
        getHandleId(),
        getPluginData(),
        getError());
  }
}
