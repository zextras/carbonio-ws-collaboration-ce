package com.zextras.chats.core.data.event;


import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class Event<T extends Event> {

  private UUID           id;
  private UUID           roomId;
  private EventType      type;
  private OffsetDateTime sentDate;

  public Event(UUID roomId, EventType type, OffsetDateTime sentDate) {
    this.id = UUID.randomUUID();
    this.roomId = roomId;
    this.type = type;
    this.sentDate = sentDate;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public EventType getType() {
    return type;
  }

  public OffsetDateTime getSentDate() {
    return sentDate;
  }
}
