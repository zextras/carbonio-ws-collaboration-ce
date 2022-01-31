// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

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
