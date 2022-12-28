// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.AudioBridgeRoom;
import java.util.UUID;

/**
 * This class represents the request for the audio bridge room and also contains one with default values.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeRoom</a>
 */
public class AudioBridgeRoomRequest extends AudioBridgeRoom implements RoomRequest {

  private static final String ROOM_DEFAULT                 = "audio_";
  private static final String DESCRIPTION_DEFAULT          = "audio_room_";
  private static final long   AUDIO_ACTIVE_PACKETS_DEFAULT = 10L;
  private static final short  AUDIO_LEVEL_AVERAGE_DEFAULT  = (short) 55;
  private static final long   SAMPLING_RATE_DEFAULT        = 16000L;

  private String request;

  public static AudioBridgeRoomRequest create(String request) {
    return new AudioBridgeRoomRequest(request);
  }

  public AudioBridgeRoomRequest(String request) {
    super.room(ROOM_DEFAULT + UUID.randomUUID());
    super.permanent(false);
    super.description(DESCRIPTION_DEFAULT + UUID.randomUUID());
    super.isPrivate(false);
    super.samplingRate(SAMPLING_RATE_DEFAULT);
    super.audioActivePackets(AUDIO_ACTIVE_PACKETS_DEFAULT);
    super.audioLevelAverage(AUDIO_LEVEL_AVERAGE_DEFAULT);
    this.request = request;
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeRoomRequest request(String request) {
    this.request = request;
    return this;
  }
}
