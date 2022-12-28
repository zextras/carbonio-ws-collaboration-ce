// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.VideoCodec;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.VideoRoom;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class represents the request for the video room and also contains one with default values.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoom</a>
 */
public class VideoRoomRequest extends VideoRoom implements RoomRequest {

  private static final String ROOM_DEFAULT           = "video_";
  private static final String DESCRIPTION_DEFAULT    = "video_room_";
  private static final short  MAX_PUBLISHERS_DEFAULT = (short) 100;
  private static final long   BITRATE_DEFAULT        = 200L;

  private String request;

  public static VideoRoomRequest create(String request) {
    return new VideoRoomRequest(request);
  }

  public VideoRoomRequest(String request) {
    super.room(ROOM_DEFAULT + UUID.randomUUID());
    super.permanent(false);
    super.description(DESCRIPTION_DEFAULT + UUID.randomUUID());
    super.isPrivate(false);
    super.publishers(MAX_PUBLISHERS_DEFAULT);
    super.bitrate(BITRATE_DEFAULT);
    super.bitrateCap(true);
    super.videoCodec(
      Arrays.stream(VideoCodec.values()).map(videoCodec -> videoCodec.toString().toLowerCase())
        .collect(Collectors.toList()));
    this.request = request;
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomRequest request(String request) {
    this.request = request;
    return this;
  }
}
