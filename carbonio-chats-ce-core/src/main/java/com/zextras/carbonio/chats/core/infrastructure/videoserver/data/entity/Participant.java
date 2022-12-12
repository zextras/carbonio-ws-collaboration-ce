package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
@JsonInclude(Include.NON_NULL)
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
