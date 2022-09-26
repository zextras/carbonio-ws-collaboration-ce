// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class UserPictureDeletedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_PICTURE_DELETED;
  private              UUID      userId;

  public UserPictureDeletedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static UserPictureDeletedEvent create(UUID from) {
    return new UserPictureDeletedEvent(from);
  }

  public UUID getUserId() {
    return userId;
  }

  public UserPictureDeletedEvent userId(UUID userId) {
    this.userId = userId;
    return this;
  }
}