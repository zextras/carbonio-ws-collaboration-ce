package com.zextras.chats.core.data.event;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zextras.chats.core.utils.CustomLocalDateTimeSerializer;
import com.zextras.chats.core.utils.CustomLocalDateTimeDeserializer;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Event<T extends Event> {

  private UUID      id;
  private UUID      roomId;
  private EventType type;
  @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
  @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
  private LocalDateTime sentDate;

  public Event(UUID roomId, EventType type, LocalDateTime sentDate) {
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

  public LocalDateTime getSentDate() {
    return sentDate;
  }
}
