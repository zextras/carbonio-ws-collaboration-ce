// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.Stream;
import java.util.List;

/**
 * This class represents the video room request to subscribe/unsubscribe to multiple streams available in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomUpdateSubscriptionsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomUpdateSubscriptionsRequest {

  public static final String UPDATE = "update";

  private String       request;
  @JsonProperty("subscribe")
  private List<Stream> subscriptions;
  @JsonProperty("unsubscribe")
  private List<Stream> unsubscriptions;

  public static VideoRoomUpdateSubscriptionsRequest create() {
    return new VideoRoomUpdateSubscriptionsRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomUpdateSubscriptionsRequest request(String request) {
    this.request = request;
    return this;
  }

  public List<Stream> getSubscriptions() {
    return subscriptions;
  }

  public VideoRoomUpdateSubscriptionsRequest subscriptions(List<Stream> subscriptions) {
    this.subscriptions = subscriptions;
    return this;
  }

  public List<Stream> getUnsubscriptions() {
    return unsubscriptions;
  }

  public VideoRoomUpdateSubscriptionsRequest unsubscriptions(List<Stream> unsubscriptions) {
    this.unsubscriptions = unsubscriptions;
    return this;
  }
}
