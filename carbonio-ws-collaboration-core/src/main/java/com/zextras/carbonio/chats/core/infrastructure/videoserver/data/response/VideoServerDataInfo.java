// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class represents the data info contained in the video server response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoServerDataInfo {

  private String id;

  public static VideoServerDataInfo create() {
    return new VideoServerDataInfo();
  }

  public String getId() {
    return id;
  }

  public VideoServerDataInfo id(String id) {
    this.id = id;
    return this;
  }
}
