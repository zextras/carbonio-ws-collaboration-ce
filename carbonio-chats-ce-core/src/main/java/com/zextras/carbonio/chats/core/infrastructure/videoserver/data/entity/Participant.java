package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity;

public class Participant {

  private String  id;
  private String  display;
  private boolean publisher;
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
}
