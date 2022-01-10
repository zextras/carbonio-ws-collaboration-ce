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
  ATTACHMENT_REMOVED("attachmentRemoved");

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
