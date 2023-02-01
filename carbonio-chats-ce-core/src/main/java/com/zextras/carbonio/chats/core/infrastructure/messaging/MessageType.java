// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

public enum MessageType {

  ROOM_NAME_CHANGED("roomNameChanged"),
  ROOM_DESCRIPTION_CHANGED("roomDescriptionChanged"),
  ROOM_PICTURE_UPDATED("roomPictureUpdated"),
  ROOM_PICTURE_DELETED("roomPictureDeleted"),
  MEMBER_ROLE_CHANGED("memberRoleChanged");

  private final String name;

  MessageType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
