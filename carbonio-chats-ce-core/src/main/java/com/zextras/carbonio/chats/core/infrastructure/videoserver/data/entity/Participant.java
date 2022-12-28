// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class represents the entity of a participant in the audio bridge room or in the video room.
 * <p>
 * Its parameters are:
 * <ul>
 *   <li>id: the participant's connection id</li>
 *   <li>display: the participant's display name</li>
 *   <li>publisher: true if the participant is a publisher in the current room, false otherwise</li>
 *   <li>talking: true if the participant is talking in the current room, false otherwise</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participant {

  private String  id;
  private String  display;
  private boolean publisher;
  private boolean talking;

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
