// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the plugin data response contained in a audiobridge plugin response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioBridgePluginData {

  private String              plugin;
  @JsonProperty("data")
  private AudioBridgeDataInfo dataInfo;

  public AudioBridgePluginData() {
  }

  public String getPlugin() {
    return plugin;
  }

  public AudioBridgeDataInfo getDataInfo() {
    return dataInfo;
  }
}