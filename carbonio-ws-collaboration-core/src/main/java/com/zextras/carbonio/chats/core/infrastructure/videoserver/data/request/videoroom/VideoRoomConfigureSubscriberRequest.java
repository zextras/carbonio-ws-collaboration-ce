// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the video room request to configure subscribers in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomSubscriberRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomConfigureSubscriberRequest extends VideoRoomRequest {

  public static final String CONFIGURE = "configure";

  private String                request;
  private List<VideoRoomStream> streams;
  private Boolean               restart;

  public static VideoRoomConfigureSubscriberRequest create() {
    return new VideoRoomConfigureSubscriberRequest();
  }

  public String getRequest() {
    return request;
  }

  public List<VideoRoomStream> getStreams() {
    return streams;
  }

  public VideoRoomConfigureSubscriberRequest streams(List<VideoRoomStream> streams) {
    this.streams = streams;
    return this;
  }

  public Boolean isRestart() {
    return restart;
  }

  public VideoRoomConfigureSubscriberRequest restart(boolean restart) {
    this.restart = restart;
    return this;
  }

  public VideoRoomConfigureSubscriberRequest request(String request) {
    this.request = request;
    return this;
  }
}
