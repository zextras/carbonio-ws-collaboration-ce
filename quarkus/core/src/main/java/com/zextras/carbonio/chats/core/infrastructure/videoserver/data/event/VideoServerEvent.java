// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents a generic event sent by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/eventhandlers.html">JanusEventHandlers</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoServerEvent {

  private String emitter;
  private Integer type;
  private Integer subtype;
  private Long timestamp;
  private Long sessionId;
  private Long handleId;
  private String opaqueId;

  @JsonProperty("event")
  private EventInfo eventInfo;

  public VideoServerEvent() {}

  public String getEmitter() {
    return emitter;
  }

  public VideoServerEvent emitter(String emitter) {
    this.emitter = emitter;
    return this;
  }

  public Integer getType() {
    return type;
  }

  public VideoServerEvent type(Integer type) {
    this.type = type;
    return this;
  }

  public Integer getSubtype() {
    return subtype;
  }

  public VideoServerEvent subtype(Integer subtype) {
    this.subtype = subtype;
    return this;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public VideoServerEvent timestamp(Long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public VideoServerEvent sessionId(Long sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public Long getHandleId() {
    return handleId;
  }

  public VideoServerEvent handleId(Long handleId) {
    this.handleId = handleId;
    return this;
  }

  public String getOpaqueId() {
    return opaqueId;
  }

  public VideoServerEvent opaqueId(String opaqueId) {
    this.opaqueId = opaqueId;
    return this;
  }

  public EventInfo getEventInfo() {
    return eventInfo;
  }

  public VideoServerEvent eventInfo(EventInfo eventInfo) {
    this.eventInfo = eventInfo;
    return this;
  }
}
