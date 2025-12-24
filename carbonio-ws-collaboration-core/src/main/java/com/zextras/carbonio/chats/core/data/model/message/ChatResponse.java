// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Base class for outgoing WebSocket events/responses to the client. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

  @JsonProperty("event")
  private ChatEvent event;

  @JsonProperty("requestId")
  private String requestId;

  @JsonProperty("sentDate")
  private OffsetDateTime sentDate;

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("message")
  private MessageDto message;

  @JsonProperty("messages")
  private List<MessageDto> messages;

  @JsonProperty("reaction")
  private String reaction;

  @JsonProperty("messageId")
  private String messageId;

  @JsonProperty("inbox")
  private List<InboxItemDto> inbox;

  @JsonProperty("error")
  private String error;

  @JsonProperty("queueId")
  private String queueId;

  @JsonProperty("extra")
  private Map<String, Object> extra;

  @JsonProperty("readStatus")
  private Map<String, String> readStatus;

  // Room-related fields
  @JsonProperty("roomName")
  private String roomName;

  @JsonProperty("roomType")
  private String roomType;

  @JsonProperty("memberIds")
  private List<String> memberIds;

  // Attachment fields
  @JsonProperty("attachment")
  private MNTAttachmentDto attachment;

  @JsonProperty("attachments")
  private List<MNTAttachmentDto> attachments;

  @JsonProperty("attachmentId")
  private String attachmentId;

  @JsonProperty("attachmentIds")
  private List<String> attachmentIds;

  public ChatResponse() {
    this.sentDate = OffsetDateTime.now();
  }

  public static ChatResponse create() {
    return new ChatResponse();
  }

  public static ChatResponse create(ChatEvent event) {
    return new ChatResponse().event(event);
  }

  public static ChatResponse createError(String errorMessage) {
    return new ChatResponse().event(ChatEvent.ERROR).error(errorMessage);
  }

  public ChatEvent getEvent() {
    return event;
  }

  public ChatResponse event(ChatEvent event) {
    this.event = event;
    return this;
  }

  public String getRequestId() {
    return requestId;
  }

  public ChatResponse requestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public OffsetDateTime getSentDate() {
    return sentDate;
  }

  public ChatResponse sentDate(OffsetDateTime sentDate) {
    this.sentDate = sentDate;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public ChatResponse roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public ChatResponse userId(String userId) {
    this.userId = userId;
    return this;
  }

  public MessageDto getMessage() {
    return message;
  }

  public ChatResponse message(MessageDto message) {
    this.message = message;
    return this;
  }

  public List<MessageDto> getMessages() {
    return messages;
  }

  public ChatResponse messages(List<MessageDto> messages) {
    this.messages = messages;
    return this;
  }

  public String getReaction() {
    return reaction;
  }

  public ChatResponse reaction(String reaction) {
    this.reaction = reaction;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public ChatResponse messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public List<InboxItemDto> getInbox() {
    return inbox;
  }

  public ChatResponse inbox(List<InboxItemDto> inbox) {
    this.inbox = inbox;
    return this;
  }

  public String getError() {
    return error;
  }

  public ChatResponse error(String error) {
    this.error = error;
    return this;
  }

  public String getQueueId() {
    return queueId;
  }

  public ChatResponse queueId(String queueId) {
    this.queueId = queueId;
    return this;
  }

  public Map<String, Object> getExtra() {
    return extra;
  }

  public ChatResponse extra(Map<String, Object> extra) {
    this.extra = extra;
    return this;
  }

  public Map<String, String> getReadStatus() {
    return readStatus;
  }

  public ChatResponse readStatus(Map<String, String> readStatus) {
    this.readStatus = readStatus;
    return this;
  }

  public String getRoomName() {
    return roomName;
  }

  public ChatResponse roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  public String getRoomType() {
    return roomType;
  }

  public ChatResponse roomType(String roomType) {
    this.roomType = roomType;
    return this;
  }

  public List<String> getMemberIds() {
    return memberIds;
  }

  public ChatResponse memberIds(List<String> memberIds) {
    this.memberIds = memberIds;
    return this;
  }

  public MNTAttachmentDto getAttachment() {
    return attachment;
  }

  public ChatResponse attachment(MNTAttachmentDto attachment) {
    this.attachment = attachment;
    return this;
  }

  public List<MNTAttachmentDto> getAttachments() {
    return attachments;
  }

  public ChatResponse attachments(List<MNTAttachmentDto> attachments) {
    this.attachments = attachments;
    return this;
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public ChatResponse attachmentId(String attachmentId) {
    this.attachmentId = attachmentId;
    return this;
  }

  public List<String> getAttachmentIds() {
    return attachmentIds;
  }

  public ChatResponse attachmentIds(List<String> attachmentIds) {
    this.attachmentIds = attachmentIds;
    return this;
  }
}
