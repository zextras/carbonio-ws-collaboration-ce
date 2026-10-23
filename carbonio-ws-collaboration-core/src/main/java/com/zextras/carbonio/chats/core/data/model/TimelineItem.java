// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import java.time.OffsetDateTime;

/**
 * Unified timeline item: the union of a {@link Message} and a {@link SystemEvent} exposing the
 * common {@code (createdAt, id)} sort key, so pagination and merging operate on one abstraction
 * instead of two disjoint id-spaces.
 */
public sealed interface TimelineItem {

  String id();

  OffsetDateTime createdAt();

  record MessageItem(Message message) implements TimelineItem {
    @Override
    public String id() {
      return message.getId();
    }

    @Override
    public OffsetDateTime createdAt() {
      return message.getCreatedAt();
    }
  }

  record SystemEventItem(SystemEvent event) implements TimelineItem {
    @Override
    public String id() {
      return event.getId();
    }

    @Override
    public OffsetDateTime createdAt() {
      return event.getCreatedAt();
    }
  }
}
