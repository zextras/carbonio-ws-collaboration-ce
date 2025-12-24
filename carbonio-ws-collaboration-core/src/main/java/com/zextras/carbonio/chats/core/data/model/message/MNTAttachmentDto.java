// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/** DTO representing a file attachment for WebSocket communication. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MNTAttachmentDto {

  @JsonProperty("id")
  private String id;

  @JsonProperty("messageId")
  private String messageId;

  @JsonProperty("fileName")
  private String fileName;

  @JsonProperty("mimeType")
  private String mimeType;

  @JsonProperty("fileSize")
  private Long fileSize;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("createdAt")
  private OffsetDateTime createdAt;

  public MNTAttachmentDto() {}

  public static MNTAttachmentDto create() {
    return new MNTAttachmentDto();
  }

  public String getId() {
    return id;
  }

  public MNTAttachmentDto id(String id) {
    this.id = id;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public MNTAttachmentDto messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public String getFileName() {
    return fileName;
  }

  public MNTAttachmentDto fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public MNTAttachmentDto mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public MNTAttachmentDto fileSize(Long fileSize) {
    this.fileSize = fileSize;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public MNTAttachmentDto userId(String userId) {
    this.userId = userId;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public MNTAttachmentDto createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
