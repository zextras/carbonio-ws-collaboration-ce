// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the plugin data response contained in a video room plugin response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoRoomPluginData {

  private String            plugin;
  @JsonProperty("data")
  private VideoRoomDataInfo dataInfo;

  public static VideoRoomPluginData create() {
    return new VideoRoomPluginData();
  }

  public String getPlugin() {
    return plugin;
  }

  public VideoRoomPluginData plugin(String plugin) {
    this.plugin = plugin;
    return this;
  }

  public VideoRoomDataInfo getDataInfo() {
    return dataInfo;
  }

  public VideoRoomPluginData dataInfo(VideoRoomDataInfo dataInfo) {
    this.dataInfo = dataInfo;
    return this;
  }
}
