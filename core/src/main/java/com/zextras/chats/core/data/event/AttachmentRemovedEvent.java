package com.zextras.chats.core.data.event;

import java.util.UUID;

public class AttachmentRemovedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ATTACHMENT_REMOVED;

  private UUID from;

  public AttachmentRemovedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static AttachmentRemovedEvent create(UUID roomId) {
    return new AttachmentRemovedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public AttachmentRemovedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
