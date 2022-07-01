// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserPictureChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_PICTURE_CHANGED;

  private UUID from;

  public UserPictureChangedEvent() {
    super(EVENT_TYPE);
  }

  public static UserPictureChangedEvent create() {
    return new UserPictureChangedEvent();
  }

  public UUID getFrom() {
    return from;
  }

  public UserPictureChangedEvent from(UUID from) {
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
    UserPictureChangedEvent that = (UserPictureChangedEvent) o;
    return Objects.equals(getType(), that.getType()) &&
      Objects.equals(from, that.from);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), from);
  }
}
