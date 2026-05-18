// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

public class AttachmentFilter {

  @Nullable private String userId;
  @Nullable private String mimeType;
  @Nullable private OffsetDateTime createdAfter;
  @Nullable private OffsetDateTime createdBefore;
  @Nullable private Long minSize;
  @Nullable private Long maxSize;
  private String sortBy;
  private String order;

  public AttachmentFilter() {
    this.sortBy = "created_at";
    this.order = "desc";
  }

  public static AttachmentFilter create() {
    return new AttachmentFilter();
  }

  public AttachmentFilter userId(@Nullable String userId) {
    this.userId = userId;
    return this;
  }

  public AttachmentFilter mimeType(@Nullable String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public AttachmentFilter createdAfter(@Nullable OffsetDateTime createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  public AttachmentFilter createdBefore(@Nullable OffsetDateTime createdBefore) {
    this.createdBefore = createdBefore;
    return this;
  }

  public AttachmentFilter minSize(@Nullable Long minSize) {
    this.minSize = minSize;
    return this;
  }

  public AttachmentFilter maxSize(@Nullable Long maxSize) {
    this.maxSize = maxSize;
    return this;
  }

  public AttachmentFilter sortBy(@Nullable String sortBy) {
    this.sortBy = sortBy != null ? sortBy : "created_at";
    return this;
  }

  public AttachmentFilter order(@Nullable String order) {
    this.order = order != null ? order : "desc";
    return this;
  }

  @Nullable
  public String getUserId() {
    return userId;
  }

  @Nullable
  public String getMimeType() {
    return mimeType;
  }

  @Nullable
  public OffsetDateTime getCreatedAfter() {
    return createdAfter;
  }

  @Nullable
  public OffsetDateTime getCreatedBefore() {
    return createdBefore;
  }

  @Nullable
  public Long getMinSize() {
    return minSize;
  }

  @Nullable
  public Long getMaxSize() {
    return maxSize;
  }

  public String getSortBy() {
    return sortBy;
  }

  public String getOrder() {
    return order;
  }

  public boolean isSortByCreatedAt() {
    return !"size".equals(sortBy);
  }

  public boolean isAscending() {
    return "asc".equals(order);
  }
}
