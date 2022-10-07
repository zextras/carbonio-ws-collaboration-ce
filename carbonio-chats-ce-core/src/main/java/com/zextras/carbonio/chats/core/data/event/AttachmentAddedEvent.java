// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.model.AttachmentDto;
import java.util.Objects;
import java.util.UUID;

public class AttachmentAddedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ATTACHMENT_ADDED;

  private UUID          roomId;
  private AttachmentDto attachment;

  public AttachmentAddedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static AttachmentAddedEvent create(UUID from) {
    return new AttachmentAddedEvent(from);
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
