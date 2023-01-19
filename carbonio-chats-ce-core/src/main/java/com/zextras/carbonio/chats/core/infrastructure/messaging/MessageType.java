// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

public enum MessageType {

  CHANGED_ROOM_NAME("changedRoomName"),
  CHANGED_ROOM_DESCRIPTION("changedRoomDescription"),
  UPDATED_ROOM_PICTURE("updatedRoomPicture"),
  DELETED_ROOM_PICTURE("deletedRoomPicture"),
  CHANGED_MEMBER_ROLE("changedMemberRole");

  private final String name;

  MessageType(String name) {
    this.name = name;

  }

  public String getName() {
    return name;
  }

}
