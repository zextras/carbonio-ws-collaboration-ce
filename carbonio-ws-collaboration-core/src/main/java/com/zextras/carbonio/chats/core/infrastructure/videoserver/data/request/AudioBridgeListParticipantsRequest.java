// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to list all participants in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeListParticipantsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeListParticipantsRequest extends AudioBridgeRequest {

  public static final String LIST_PARTICIPANTS = "listparticipants";

  private String request;
  private String room;

  public static AudioBridgeListParticipantsRequest create() {
    return new AudioBridgeListParticipantsRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeListParticipantsRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeListParticipantsRequest room(String room) {
    this.room = room;
    return this;
  }
}
