// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;


import java.time.OffsetDateTime;
import java.util.Objects;

public class DomainEvent {

  private EventType eventType;
  private OffsetDateTime       sentDate;

  public DomainEvent(EventType eventType) {
    this.eventType = eventType;
    this.sentDate = OffsetDateTime.now();
  }

  public EventType getType() {
    return eventType;
  }

  public OffsetDateTime getSentDate() {
    return sentDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DomainEvent that = (DomainEvent) o;
    return Objects.equals(getType(), that.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType());
  }
}
