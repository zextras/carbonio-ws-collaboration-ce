package com.zextras.team.core.events;


public class RoomCreatedEvent {

  private String roomId;

  public RoomCreatedEvent(String roomId) {
    this.roomId = roomId;
  }
}
