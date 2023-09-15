// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the data contained in a generic event sent by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/eventhandlers.html">JanusEventHandlers</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventData {

  public EventData() {
  }

  private String           event;
  @JsonProperty("audiobridge")
  private String           audioBridge;
  @JsonProperty("videoroom")
  private String           videoRoom;
  private String           room;
  @JsonProperty("streams")
  private List<StreamData> streamList;
  private String           id;

  public String getEvent() {
    return event;
  }

  public String getAudioBridge() {
    return audioBridge;
  }

  public String getVideoRoom() {
    return videoRoom;
  }

  public String getRoom() {
    return room;
  }

  public List<StreamData> getStreamList() {
    return streamList;
  }

  public String getId() {
    return id;
  }
}
