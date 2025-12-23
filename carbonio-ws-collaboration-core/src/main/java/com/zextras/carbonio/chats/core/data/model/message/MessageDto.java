// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** DTO representing a chat message for WebSocket communication. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto {

  @JsonProperty("id")
  private String id;

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("senderId")
  private String senderId;

  @JsonProperty("text")
  private String text;

  @JsonProperty("replyToId")
  private String replyToId;

  @JsonProperty("replyTo")
  private MessageDto replyTo;

  @JsonProperty("forwardedFromId")
  private String forwardedFromId;

  @JsonProperty("forwardedFrom")
  private MessageDto forwardedFrom;

  @JsonProperty("stanzaId")
  private String stanzaId;

  @JsonProperty("edited")
  private Boolean edited;

  @JsonProperty("editedAt")
  private OffsetDateTime editedAt;

  @JsonProperty("deleted")
  private Boolean deleted;

  @JsonProperty("createdAt")
  private OffsetDateTime createdAt;

  @JsonProperty("reactions")
  private Map<String, List<String>> reactions; // emoji -> list of user IDs

  public MessageDto() {}

  public static MessageDto create() {
    return new MessageDto();
  }

  public String getId() {
    return id;
  }

  public MessageDto id(String id) {
    this.id = id;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public MessageDto roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getSenderId() {
    return senderId;
  }

  public MessageDto senderId(String senderId) {
    this.senderId = senderId;
    return this;
  }

  public String getText() {
    return text;
  }

  public MessageDto text(String text) {
    this.text = text;
    return this;
  }

  public String getReplyToId() {
    return replyToId;
  }

  public MessageDto replyToId(String replyToId) {
    this.replyToId = replyToId;
    return this;
  }

  public MessageDto getReplyTo() {
    return replyTo;
  }

  public MessageDto replyTo(MessageDto replyTo) {
    this.replyTo = replyTo;
    return this;
  }

  public String getForwardedFromId() {
    return forwardedFromId;
  }

  public MessageDto forwardedFromId(String forwardedFromId) {
    this.forwardedFromId = forwardedFromId;
    return this;
  }

  public MessageDto getForwardedFrom() {
    return forwardedFrom;
  }

  public MessageDto forwardedFrom(MessageDto forwardedFrom) {
    this.forwardedFrom = forwardedFrom;
    return this;
  }

  public String getStanzaId() {
    return stanzaId;
  }

  public MessageDto stanzaId(String stanzaId) {
    this.stanzaId = stanzaId;
    return this;
  }

  public Boolean getEdited() {
    return edited;
  }

  public MessageDto edited(Boolean edited) {
    this.edited = edited;
    return this;
  }

  public OffsetDateTime getEditedAt() {
    return editedAt;
  }

  public MessageDto editedAt(OffsetDateTime editedAt) {
    this.editedAt = editedAt;
    return this;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public MessageDto deleted(Boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public MessageDto createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Map<String, List<String>> getReactions() {
    return reactions;
  }

  public MessageDto reactions(Map<String, List<String>> reactions) {
    this.reactions = reactions;
    return this;
  }
}
