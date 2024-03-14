// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import java.time.OffsetDateTime;

public class PaginationFilter {

  private String id;
  private OffsetDateTime createdAt;

  public PaginationFilter() {}

  public PaginationFilter(String id, OffsetDateTime createdAt) {
    this.id = id;
    this.createdAt = createdAt;
  }

  public static PaginationFilter create(String id, OffsetDateTime createdAt) {
    return new PaginationFilter(id, createdAt);
  }

  public String getId() {
    return id;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
