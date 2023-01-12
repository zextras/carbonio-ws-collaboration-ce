// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This abstract class a generic response provided by VideoServer
 * <p>
 * It's composed of at least one mandatory field:
 * <ul>
 *   <li>janus: indicates the status/error of the response</li>
 * </ul>
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class VideoServerResponse {

  @JsonProperty("janus")
  private String status;

  public String getStatus() {
    return status;
  }
}
