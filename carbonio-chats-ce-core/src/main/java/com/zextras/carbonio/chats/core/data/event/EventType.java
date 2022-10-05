// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {

  ROOM_CREATED("roomCreated"),
  ROOM_UPDATED("roomUpdated"),
  ROOM_DELETED("roomDeleted"),
  ROOM_HASH_RESET_EVENT("roomHashResetEvent"),
  ROOM_OWNER_CHANGED("roomOwnerChanged"),
  ROOM_PICTURE_CHANGED("roomPictureChanged"),
  ROOM_PICTURE_DELETED("roomPictureDeleted"),
  ROOM_MEMBER_ADDED("roomMemberAdded"),
  ROOM_MEMBER_REMOVED("roomMemberRemoved"),
  ATTACHMENT_ADDED("attachmentAdded"),
  ATTACHMENT_REMOVED("attachmentRemoved"),
  USER_MUTED("userMuted"),
  USER_UNMUTED("userUnmuted"),
  USER_PICTURE_CHANGED("userPictureChanged"),
  USER_PICTURE_DELETED("userPictureDeleted"),
  CLEARED_ROOM_EVENT("clearedRoomEvent");


  private final String description;

  EventType(String description) {
    this.description = description;
  }

  @JsonValue
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return this.description;
  }
}
