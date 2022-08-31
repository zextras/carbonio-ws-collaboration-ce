// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;


import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class DomainEvent {

  private UUID           id;
  private EventType      type;
  private UUID           from;
  private OffsetDateTime sentDate;

  public DomainEvent(EventType type, UUID from) {
    this.id = UUID.randomUUID();
    this.type = type;
    this.sentDate = OffsetDateTime.now();
    this.from = from;
  }

  public UUID getId() {
    return id;
  }

  public EventType getType() {
    return type;
  }

  public OffsetDateTime getSentDate() {
    return sentDate;
  }

  public UUID getFrom() {
    return from;
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
    return Objects.equals(getType(), that.getType()) &&
      Objects.equals(getFrom(), that.getFrom());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getFrom());
  }
}
