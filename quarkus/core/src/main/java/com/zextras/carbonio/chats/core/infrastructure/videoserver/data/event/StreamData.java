// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the list of streams contained in the event data of a generic event sent by
 * VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/eventhandlers.html">JanusEventHandlers</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StreamData {

  public StreamData() {}

  private String type;
  private Boolean active;
  private Long mindex;
  private String mid;
  private Boolean ready;
  private Boolean send;
  private String feedId;
  private String feedMid;
  private String codec;

  public String getType() {
    return type;
  }

  public StreamData type(String type) {
    this.type = type;
    return this;
  }

  public Boolean getActive() {
    return active;
  }

  public StreamData active(Boolean active) {
    this.active = active;
    return this;
  }

  public Long getMindex() {
    return mindex;
  }

  public StreamData mindex(Long mindex) {
    this.mindex = mindex;
    return this;
  }

  public String getMid() {
    return mid;
  }

  public StreamData mid(String mid) {
    this.mid = mid;
    return this;
  }

  public Boolean getReady() {
    return ready;
  }

  public StreamData ready(Boolean ready) {
    this.ready = ready;
    return this;
  }

  public Boolean getSend() {
    return send;
  }

  public StreamData send(Boolean send) {
    this.send = send;
    return this;
  }

  public String getFeedId() {
    return feedId;
  }

  public StreamData feedId(String feedId) {
    this.feedId = feedId;
    return this;
  }

  public String getFeedMid() {
    return feedMid;
  }

  public StreamData feedMid(String feedMid) {
    this.feedMid = feedMid;
    return this;
  }

  public String getCodec() {
    return codec;
  }

  public StreamData codec(String codec) {
    this.codec = codec;
    return this;
  }
}
