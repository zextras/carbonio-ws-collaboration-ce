// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

public class PaginationFilter {

  private String id;
  @Nullable private OffsetDateTime createdAt;
  @Nullable private Long size;
  @Nullable private String sortBy;
  @Nullable private String order;

  public PaginationFilter() {}

  public PaginationFilter(
      String id,
      @Nullable OffsetDateTime createdAt,
      @Nullable Long size,
      @Nullable String sortBy,
      @Nullable String order) {
    this.id = id;
    this.createdAt = createdAt;
    this.size = size;
    this.sortBy = sortBy;
    this.order = order;
  }

  public static PaginationFilter create(
      String id, @Nullable OffsetDateTime createdAt, @Nullable Long size) {
    return new PaginationFilter(id, createdAt, size, null, null);
  }

  public static PaginationFilter create(
      String id,
      @Nullable OffsetDateTime createdAt,
      @Nullable Long size,
      @Nullable String sortBy,
      @Nullable String order) {
    return new PaginationFilter(id, createdAt, size, sortBy, order);
  }

  public String getId() {
    return id;
  }

  @Nullable
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @Nullable
  public Long getSize() {
    return size;
  }

  @Nullable
  public String getSortBy() {
    return sortBy;
  }

  @Nullable
  public String getOrder() {
    return order;
  }
}
