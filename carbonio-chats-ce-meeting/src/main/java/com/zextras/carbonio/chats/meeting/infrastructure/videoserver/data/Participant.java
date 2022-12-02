package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Participant {

  @JsonProperty
  private String  id;
  @JsonProperty
  private String  display;
  @JsonProperty
  private boolean publisher;
  @JsonProperty
  private boolean talking;

  public Participant() {
  }

  public Participant(String id, String display, boolean publisher, boolean talking) {
    this.id = id;
    this.display = display;
    this.publisher = publisher;
    this.talking = talking;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplay() {
    return display;
  }

  public void setDisplay(String display) {
    this.display = display;
  }

  public boolean isPublisher() {
    return publisher;
  }

  public void setPublisher(boolean publisher) {
    this.publisher = publisher;
  }

  public boolean isTalking() {
    return talking;
  }

  public void setTalking(boolean talking) {
    this.talking = talking;
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return this.toString();
    }
  }
}
