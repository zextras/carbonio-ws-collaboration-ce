// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class represents the plugin error response contained in a videoroom/audiobridge plugin response provided by
 * VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginErrorResponse {

  private Long   code;
  private String reason;

  public PluginErrorResponse() {
  }

  public long getCode() {
    return code;
  }

  public String getReason() {
    return reason;
  }
}
