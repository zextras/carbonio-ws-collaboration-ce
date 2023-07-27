// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserPictureDeleted extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_PICTURE_DELETED;

  private UUID userId;

  public UserPictureDeleted() {
    super(EVENT_TYPE);
  }

  public static UserPictureDeleted create() {
    return new UserPictureDeleted();
  }

  public UUID getUserId() {
    return userId;
  }

  public UserPictureDeleted userId(UUID userId) {
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
    return Objects.equals(getUserId(), ((UserPictureDeleted) o).getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getUserId());
  }
}
