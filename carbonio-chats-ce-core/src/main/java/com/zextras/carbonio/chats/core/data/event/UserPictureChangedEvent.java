// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class UserPictureChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_PICTURE_CHANGED;

  private UUID userId;

  public UserPictureChangedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static UserPictureChangedEvent create(UUID from, @Nullable String sessionId) {
    return new UserPictureChangedEvent(from, sessionId);
  }

  public UUID getUserId() {
    return userId;
  }

  public UserPictureChangedEvent userId(UUID userId) {
    this.userId = userId;
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
    return Objects.equals(getUserId(), ((UserPictureChangedEvent) o).getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getUserId());
  }
}
