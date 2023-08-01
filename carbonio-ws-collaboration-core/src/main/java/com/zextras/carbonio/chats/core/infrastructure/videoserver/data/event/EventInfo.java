// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;

/**
 * This class represents the info contained in a generic event sent by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/eventhandlers.html">JanusEventHandlers</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventInfo {

  public EventInfo() {
  }

  private String                name;
  private String                plugin;
  @JsonProperty("data")
  private EventData             eventData;
  private String                owner;
  @JsonProperty("jsep")
  private RtcSessionDescription rtcSessionDescription;

  public String getName() {
    return name;
  }

  public String getPlugin() {
    return plugin;
  }

  public EventData getEventData() {
    return eventData;
  }

  public String getOwner() {
    return owner;
  }

  public RtcSessionDescription getRtcSessionDescription() {
    return rtcSessionDescription;
  }
}
