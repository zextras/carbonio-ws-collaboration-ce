// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Base class for incoming WebSocket requests from the client. Uses a flexible Map-based payload to
 * handle different action types.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRequest {

  @JsonProperty("action")
  private ChatAction action;

  @JsonProperty("requestId")
  private String requestId;

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("messageId")
  private String messageId;

  @JsonProperty("text")
  private String text;

  @JsonProperty("replyToId")
  private String replyToId;

  @JsonProperty("forwardedFromId")
  private String forwardedFromId;

  @JsonProperty("targetRoomId")
  private String targetRoomId;

  @JsonProperty("reaction")
  private String reaction;

  @JsonProperty("beforeMessageId")
  private String beforeMessageId;

  @JsonProperty("afterMessageId")
  private String afterMessageId;

  @JsonProperty("limit")
  private Integer limit;

  @JsonProperty("fromDate")
  private OffsetDateTime fromDate;

  @JsonProperty("toDate")
  private OffsetDateTime toDate;

  @JsonProperty("searchText")
  private String searchText;

  @JsonProperty("extra")
  private Map<String, Object> extra;

  // Room creation fields
  @JsonProperty("roomName")
  private String roomName;

  @JsonProperty("roomDescription")
  private String roomDescription;

  @JsonProperty("roomType")
  private String roomType;

  @JsonProperty("memberIds")
  private List<String> memberIds;

  public ChatRequest() {}

  public ChatAction getAction() {
    return action;
  }

  public ChatRequest action(ChatAction action) {
    this.action = action;
    return this;
  }

  public String getRequestId() {
    return requestId;
  }

  public ChatRequest requestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public ChatRequest roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public ChatRequest messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public String getText() {
    return text;
  }

  public ChatRequest text(String text) {
    this.text = text;
    return this;
  }

  public String getReplyToId() {
    return replyToId;
  }

  public ChatRequest replyToId(String replyToId) {
    this.replyToId = replyToId;
    return this;
  }

  public String getForwardedFromId() {
    return forwardedFromId;
  }

  public ChatRequest forwardedFromId(String forwardedFromId) {
    this.forwardedFromId = forwardedFromId;
    return this;
  }

  public String getTargetRoomId() {
    return targetRoomId;
  }

  public ChatRequest targetRoomId(String targetRoomId) {
    this.targetRoomId = targetRoomId;
    return this;
  }

  public String getReaction() {
    return reaction;
  }

  public ChatRequest reaction(String reaction) {
    this.reaction = reaction;
    return this;
  }

  public String getBeforeMessageId() {
    return beforeMessageId;
  }

  public ChatRequest beforeMessageId(String beforeMessageId) {
    this.beforeMessageId = beforeMessageId;
    return this;
  }

  public String getAfterMessageId() {
    return afterMessageId;
  }

  public ChatRequest afterMessageId(String afterMessageId) {
    this.afterMessageId = afterMessageId;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public ChatRequest limit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public OffsetDateTime getFromDate() {
    return fromDate;
  }

  public ChatRequest fromDate(OffsetDateTime fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  public OffsetDateTime getToDate() {
    return toDate;
  }

  public ChatRequest toDate(OffsetDateTime toDate) {
    this.toDate = toDate;
    return this;
  }

  public String getSearchText() {
    return searchText;
  }

  public ChatRequest searchText(String searchText) {
    this.searchText = searchText;
    return this;
  }

  public Map<String, Object> getExtra() {
    return extra;
  }

  public ChatRequest extra(Map<String, Object> extra) {
    this.extra = extra;
    return this;
  }

  public String getRoomName() {
    return roomName;
  }

  public ChatRequest roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  public String getRoomDescription() {
    return roomDescription;
  }

  public ChatRequest roomDescription(String roomDescription) {
    this.roomDescription = roomDescription;
    return this;
  }

  public String getRoomType() {
    return roomType;
  }

  public ChatRequest roomType(String roomType) {
    this.roomType = roomType;
    return this;
  }

  public List<String> getMemberIds() {
    return memberIds;
  }

  public ChatRequest memberIds(List<String> memberIds) {
    this.memberIds = memberIds;
    return this;
  }
}
