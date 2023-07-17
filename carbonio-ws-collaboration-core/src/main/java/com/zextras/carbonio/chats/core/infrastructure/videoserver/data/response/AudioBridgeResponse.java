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
 * This class represents an audio bridge response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeResponse {

  public static final String CREATED   = "created";
  public static final String EDITED    = "edited";
  public static final String DESTROYED = "destroyed";
  public static final String ACK       = "ack";

  public static final String SUCCESS      = "success";
  public static final String PARTICIPANTS = "participants";

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

  public AudioBridgeResponse() {
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

  public String getAudioBridge() {
    return getPluginData().getData().getAudioBridge();
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

    @JsonProperty("audiobridge")
    private String            audioBridge;
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

    public String getAudioBridge() {
      return audioBridge;
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
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private static class Participant {

    private String  id;
    private String  display;
    private Boolean setup;
    private Boolean muted;
    private Boolean talking;
    private Integer spatialPosition;

    public Participant() {
    }

    public String getId() {
      return id;
    }

    public String getDisplay() {
      return display;
    }

    public boolean isSetup() {
      return setup;
    }

    public boolean isMuted() {
      return muted;
    }

    public boolean isTalking() {
      return talking;
    }

    public int getSpatialPosition() {
      return spatialPosition;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private static class Room {

    private String  room;
    private String  description;
    private Boolean pinRequired;
    private Long    samplingRate;
    private Boolean spatialAudio;
    private Boolean record;
    private Boolean muted;
    private Integer numParticipants;

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

    public long getSamplingRate() {
      return samplingRate;
    }

    public boolean isSpatialAudio() {
      return spatialAudio;
    }

    public boolean isRecord() {
      return record;
    }

    public boolean isMuted() {
      return muted;
    }

    public int getNumParticipants() {
      return numParticipants;
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
