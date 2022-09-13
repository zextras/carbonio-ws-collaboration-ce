// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

public enum EventType {

  ROOM_CREATED("roomCreated"),
  ROOM_UPDATED("roomUpdated"),
  ROOM_DELETED("roomDeleted"),
  ROOM_HASH_RESET_EVENT("roomHashResetEvent"),
  ROOM_OWNER_CHANGED("roomOwnerChanged"),
  ROOM_PICTURE_CHANGED("roomPictureChanged"),
  ROOM_MEMBER_ADDED("roomMemberAdded"),
  ROOM_MEMBER_REMOVED("roomMemberRemoved"),
  ATTACHMENT_ADDED("attachmentAdded"),
  ATTACHMENT_REMOVED("attachmentRemoved"),
  USER_MUTED("userMuted"),
  USER_UNMUTED("userUnmuted"),
  USER_PICTURE_CHANGED("userPictureChanged"),
  USER_PICTURE_DELETED("userPictureDeleted");


  private final String name;

  EventType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
