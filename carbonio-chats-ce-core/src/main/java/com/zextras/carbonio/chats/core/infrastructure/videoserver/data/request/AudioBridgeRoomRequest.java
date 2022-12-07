package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.AudioBridgeRoom;
import java.util.UUID;

public class AudioBridgeRoomRequest extends AudioBridgeRoom implements RoomRequest {

  private static final String ROOM_DEFAULT                 = "audio_";
  private static final String DESCRIPTION_DEFAULT          = "audio_room_";
  private static final long   AUDIO_ACTIVE_PACKETS_DEFAULT = 10L;
  private static final short  AUDIO_LEVEL_AVERAGE_DEFAULT  = (short) 55;
  private static final long   SAMPLING_RATE_DEFAULT        = 16000L;

  private String request;

  public AudioBridgeRoomRequest() {
  }

  public static AudioBridgeRoomRequest create() {
    return new AudioBridgeRoomRequest();
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
