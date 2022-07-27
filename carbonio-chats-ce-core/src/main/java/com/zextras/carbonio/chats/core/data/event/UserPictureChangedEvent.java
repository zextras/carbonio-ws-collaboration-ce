// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserPictureChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_PICTURE_CHANGED;

  public UserPictureChangedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static UserPictureChangedEvent create(UUID from) {
    return new UserPictureChangedEvent(from);
  }
}
