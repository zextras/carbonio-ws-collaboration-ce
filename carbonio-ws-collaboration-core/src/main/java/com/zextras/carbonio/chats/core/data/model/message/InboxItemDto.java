// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO representing an inbox item (room with last message and unread count). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboxItemDto {

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("roomName")
  private String roomName;

  @JsonProperty("roomType")
  private String roomType;

  @JsonProperty("lastMessage")
  private MessageDto lastMessage;

  @JsonProperty("unreadCount")
  private Long unreadCount;

  @JsonProperty("muted")
  private Boolean muted;

  @JsonProperty("members")
  private java.util.List<String> members;

  public InboxItemDto() {}

  public static InboxItemDto create() {
    return new InboxItemDto();
  }

  public String getRoomId() {
    return roomId;
  }

  public InboxItemDto roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getRoomName() {
    return roomName;
  }

  public InboxItemDto roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  public String getRoomType() {
    return roomType;
  }

  public InboxItemDto roomType(String roomType) {
    this.roomType = roomType;
    return this;
  }

  public MessageDto getLastMessage() {
    return lastMessage;
  }

  public InboxItemDto lastMessage(MessageDto lastMessage) {
    this.lastMessage = lastMessage;
    return this;
  }

  public Long getUnreadCount() {
    return unreadCount;
  }

  public InboxItemDto unreadCount(Long unreadCount) {
    this.unreadCount = unreadCount;
    return this;
  }

  public Boolean getMuted() {
    return muted;
  }

  public InboxItemDto muted(Boolean muted) {
    this.muted = muted;
    return this;
  }

  public java.util.List<String> getMembers() {
    return members;
  }

  public InboxItemDto members(java.util.List<String> members) {
    this.members = members;
    return this;
  }
}
