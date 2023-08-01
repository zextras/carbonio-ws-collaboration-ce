// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class represents the participant data contained in the video room response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoRoomDataParticipant {

  private String  id;
  private String  display;
  private Boolean publisher;
  private Boolean talking;

  public VideoRoomDataParticipant() {
  }

  public String getId() {
    return id;
  }

  public String getDisplay() {
    return display;
  }

  public boolean isPublisher() {
    return publisher;
  }

  public boolean isTalking() {
    return talking;
  }
}
