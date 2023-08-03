// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the participant data contained in the audio bridge response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeDataParticipant {

  private String  id;
  private String  display;
  private Boolean setup;
  private Boolean muted;
  private Boolean talking;
  private Integer spatialPosition;

  public AudioBridgeDataParticipant() {
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
