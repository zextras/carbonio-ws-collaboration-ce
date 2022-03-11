// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttachmentAddedEvent that = (AttachmentAddedEvent) o;
    return Objects.equals(getType(), that.getType()) &&
      Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(from, that.from);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from);
  }
}
