// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class DomainEvent {

  private UUID                 id;
  private EventTypeDescription eventTypeDescription;
  private UUID                 from;
  private String               sessionId;
  private OffsetDateTime       sentDate;

  public DomainEvent(EventTypeDescription eventTypeDescription, UUID from, @Nullable String sessionId) {
    this.id = UUID.randomUUID();
    this.eventTypeDescription = eventTypeDescription;
    this.sessionId = sessionId;
    this.sentDate = OffsetDateTime.now();
    this.from = from;
  }

  public UUID getId() {
    return id;
  }

  public EventTypeDescription getType() {
    return eventTypeDescription;
  }

  public OffsetDateTime getSentDate() {
    return sentDate;
  }

  public UUID getFrom() {
    return from;
  }

  public String getSessionId() {
    return sessionId;
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
      Objects.equals(getFrom(), that.getFrom()) &&
      Objects.equals(getSessionId(), that.getSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getFrom(), getSessionId());
  }
}
