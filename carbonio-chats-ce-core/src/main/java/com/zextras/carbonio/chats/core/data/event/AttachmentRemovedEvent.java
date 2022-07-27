// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class AttachmentRemovedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ATTACHMENT_REMOVED;

  private UUID roomId;

  public AttachmentRemovedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static AttachmentRemovedEvent create(UUID from) {
    return new AttachmentRemovedEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public AttachmentRemovedEvent roomId(UUID roomId) {
    this.roomId = roomId;
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
    if (!super.equals(o)) {
      return false;
    }
    AttachmentRemovedEvent that = (AttachmentRemovedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId());
  }
}
