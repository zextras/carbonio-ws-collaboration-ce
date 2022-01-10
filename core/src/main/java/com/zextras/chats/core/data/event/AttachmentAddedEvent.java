package com.zextras.chats.core.data.event;

import java.util.UUID;

public class AttachmentAddedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ATTACHMENT_ADDED;

  private UUID from;

  public AttachmentAddedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static AttachmentAddedEvent create(UUID roomId) {
    return new AttachmentAddedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public AttachmentAddedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
