package com.zextras.chats.core.data.event;

public enum EventType {

  ROOM_CREATED("roomCreated"),
  ROOM_UPDATED("roomUpdated");

  private final String name;

  EventType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
