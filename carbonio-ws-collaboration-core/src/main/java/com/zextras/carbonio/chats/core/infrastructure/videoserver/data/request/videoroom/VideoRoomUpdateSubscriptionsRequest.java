// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the video room request to subscribe/unsubscribe to multiple streams
 * available in a room.
 *
 * @see <a
 *     href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomUpdateSubscriptionsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomUpdateSubscriptionsRequest extends VideoRoomRequest {

  public static final String UPDATE = "update";

  private String request;

  @JsonProperty("subscribe")
  @JsonInclude(Include.NON_EMPTY)
  private List<Stream> subscriptions;

  @JsonProperty("unsubscribe")
  @JsonInclude(Include.NON_EMPTY)
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomUpdateSubscriptionsRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getSubscriptions(), that.getSubscriptions())
        && Objects.equals(getUnsubscriptions(), that.getUnsubscriptions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getSubscriptions(), getUnsubscriptions());
  }
}
