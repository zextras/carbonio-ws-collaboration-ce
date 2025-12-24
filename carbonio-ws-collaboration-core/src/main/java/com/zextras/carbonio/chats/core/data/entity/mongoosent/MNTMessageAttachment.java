// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;

/**
 * File attachment linked to a message. The actual blob is stored in storages service, this entity
 * holds the metadata.
 */
@Entity
@Table(name = "MONGOOSENT_MESSAGE_ATTACHMENT", schema = "CHATS")
public class MNTMessageAttachment {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "MESSAGE_ID", length = 64)
  private String messageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "MESSAGE_ID", insertable = false, updatable = false)
  private MNTMessage message;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "FILE_NAME", length = 512, nullable = false)
  private String fileName;

  @Column(name = "MIME_TYPE", length = 256, nullable = false)
  private String mimeType;

  @Column(name = "FILE_SIZE", nullable = false)
  private Long fileSize;

  @Column(name = "DELETED", nullable = false)
  private Boolean deleted = false;

  @Column(name = "CREATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  public MNTMessageAttachment() {}

  public static MNTMessageAttachment create() {
    return new MNTMessageAttachment();
  }

  public String getId() {
    return id;
  }

  public MNTMessageAttachment id(String id) {
    this.id = id;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public MNTMessageAttachment messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public MNTMessage getMessage() {
    return message;
  }

  public String getUserId() {
    return userId;
  }

  public MNTMessageAttachment userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getFileName() {
    return fileName;
  }

  public MNTMessageAttachment fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public MNTMessageAttachment mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public MNTMessageAttachment fileSize(Long fileSize) {
    this.fileSize = fileSize;
    return this;
  }

  public Boolean isDeleted() {
    return deleted;
  }

  public MNTMessageAttachment deleted(Boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
