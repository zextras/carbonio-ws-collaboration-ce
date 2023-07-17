// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents a video room response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomResponse {

  public static final String CREATED   = "created";
  public static final String EDITED    = "edited";
  public static final String DESTROYED = "destroyed";
  public static final String ACK       = "ack";

  @JsonProperty("janus")
  private String     status;
  @JsonProperty("session_id")
  private String     connectionId;
  @JsonProperty("transaction")
  private String     transactionId;
  @JsonProperty("sender")
  private String     handleId;
  @JsonProperty("plugindata")
  private PluginData pluginData;

  private Error error;

  public VideoRoomResponse() {
  }

  public String getStatus() {
    return status;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getHandleId() {
    return handleId;
  }

  public PluginData getPluginData() {
    return pluginData;
  }

  public Error getError() {
    return error;
  }

  public String getVideoRoom() {
    return getPluginData().getData().getVideoRoom();
  }

  public String getRoom() {
    return getPluginData().getData().getRoom();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class PluginData {

    private String plugin;
    private Data   data;

    public PluginData() {
    }

    public String getPlugin() {
      return plugin;
    }

    public Data getData() {
      return data;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Data {

    @JsonProperty("videoroom")
    private String            videoRoom;
    private String            room;
    private String            permanent;
    private Boolean           exists;
    private List<String>      allowed;
    @JsonProperty("list")
    private List<Room>        rooms;
    private List<Participant> participants;

    private String errorCode;
    private String error;

    public Data() {
    }

    public String getVideoRoom() {
      return videoRoom;
    }

    public String getRoom() {
      return room;
    }

    public String getPermanent() {
      return permanent;
    }

    public boolean isExists() {
      return exists;
    }

    public List<String> getAllowed() {
      return allowed;
    }

    public List<Room> getRooms() {
      return rooms;
    }

    public List<Participant> getParticipants() {
      return participants;
    }

    public String getErrorCode() {
      return errorCode;
    }

    public String getError() {
      return error;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Participant {

    private String  id;
    private String  display;
    private Boolean publisher;
    private Boolean talking;

    public Participant() {
    }

    public String getId() {
      return id;
    }

    public String getDisplay() {
      return display;
    }

    public boolean isPublisher() {
      return publisher;
    }

    public boolean isTalking() {
      return talking;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private static class Room {

    private String       room;
    private String       description;
    private Boolean      pinRequired;
    private Boolean      isPrivate;
    private Integer      maxPublishers;
    private Long         bitrate;
    private Boolean      bitrateCap;
    private Integer      firFreq;
    @JsonProperty("require_pvtid")
    private Boolean      requirePvtId;
    private Boolean      requireE2ee;
    private Boolean      dummyPublisher;
    private Boolean      notifyJoining;
    private List<String> audioCodec;
    private List<String> videoCodec;
    private Boolean      opusFec;
    private Boolean      opusDtx;
    private Boolean      record;
    private String       recDir;
    private Boolean      lockRecord;
    private Integer      numParticipants;
    @JsonProperty("audiolevel_ext")
    private Boolean      audioLevelExt;
    @JsonProperty("audiolevel_event")
    private Boolean      audioLevelEvent;
    private Long         audioActivePackets;
    private Long         audioLevelAverage;
    @JsonProperty("videoorient_ext")
    private Boolean      videoOrientExt;
    @JsonProperty("playoutdelay_ext")
    private Boolean      playOutDelayExt;
    private Boolean      transportWideCcExt;

    public Room() {
    }

    public String getRoom() {
      return room;
    }

    public String getDescription() {
      return description;
    }

    public boolean isPinRequired() {
      return pinRequired;
    }

    public boolean isPrivate() {
      return isPrivate;
    }

    public int getMaxPublishers() {
      return maxPublishers;
    }

    public long getBitrate() {
      return bitrate;
    }

    public boolean isBitrateCap() {
      return bitrateCap;
    }

    public int getFirFreq() {
      return firFreq;
    }

    public boolean isRequirePvtId() {
      return requirePvtId;
    }

    public boolean isRequireE2ee() {
      return requireE2ee;
    }

    public boolean isDummyPublisher() {
      return dummyPublisher;
    }

    public boolean isNotifyJoining() {
      return notifyJoining;
    }

    public List<String> getAudioCodec() {
      return audioCodec;
    }

    public List<String> getVideoCodec() {
      return videoCodec;
    }

    public boolean isOpusFec() {
      return opusFec;
    }

    public boolean isOpusDtx() {
      return opusDtx;
    }

    public boolean isRecord() {
      return record;
    }

    public String getRecDir() {
      return recDir;
    }

    public boolean isLockRecord() {
      return lockRecord;
    }

    public int getNumParticipants() {
      return numParticipants;
    }

    public boolean isAudioLevelExt() {
      return audioLevelExt;
    }

    public boolean isAudioLevelEvent() {
      return audioLevelEvent;
    }

    public long getAudioActivePackets() {
      return audioActivePackets;
    }

    public long getAudioLevelAverage() {
      return audioLevelAverage;
    }

    public boolean isVideoOrientExt() {
      return videoOrientExt;
    }

    public boolean isPlayOutDelayExt() {
      return playOutDelayExt;
    }

    public boolean isTransportWideCcExt() {
      return transportWideCcExt;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Error {

    private Long   code;
    private String reason;

    public Error() {
    }

    public long getCode() {
      return code;
    }

    public String getReason() {
      return reason;
    }
  }
}
