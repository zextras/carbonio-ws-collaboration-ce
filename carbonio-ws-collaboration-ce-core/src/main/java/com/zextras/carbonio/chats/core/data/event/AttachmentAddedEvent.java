// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.model.AttachmentDto;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class AttachmentAddedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ATTACHMENT_ADDED;

  private UUID          roomId;
  private AttachmentDto attachment;

  public AttachmentAddedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static AttachmentAddedEvent create(UUID from, @Nullable String sessionId) {
    return new AttachmentAddedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public AttachmentAddedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public AttachmentDto getAttachment() {
    return attachment;
  }

  public AttachmentAddedEvent attachment(AttachmentDto attachment) {
    this.attachment = attachment;
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
    AttachmentAddedEvent that = (AttachmentAddedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(getAttachment(), that.getAttachment());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getAttachment());
  }
}
