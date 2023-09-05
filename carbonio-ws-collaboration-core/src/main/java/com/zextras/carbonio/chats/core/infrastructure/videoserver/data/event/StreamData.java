// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class represents the list of streams contained in the event data of a generic event sent by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 * @see <a href="https://janus.conf.meetecho.com/docs/eventhandlers.html">JanusEventHandlers</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamData {

  public StreamData() {
  }

  private String  type;
  private Boolean active;
  private Long    mindex;
  private String  mid;
  private Boolean ready;
  private Boolean send;
  private String  feedId;
  private String  feedMid;
  private String  codec;

  public String getType() {
    return type;
  }

  public Boolean getActive() {
    return active;
  }

  public Long getMindex() {
    return mindex;
  }

  public String getMid() {
    return mid;
  }

  public Boolean getReady() {
    return ready;
  }

  public Boolean getSend() {
    return send;
  }

  public String getFeedId() {
    return feedId;
  }

  public String getFeedMid() {
    return feedMid;
  }

  public String getCodec() {
    return codec;
  }
}