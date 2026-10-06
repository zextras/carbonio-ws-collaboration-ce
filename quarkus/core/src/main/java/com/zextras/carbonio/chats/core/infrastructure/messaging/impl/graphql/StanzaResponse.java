// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql;

import jakarta.annotation.Nullable;

public record StanzaResponse(String id, String stanzaId) {

  public StanzaResponse(@Nullable String id, @Nullable String stanzaId) {
    this.id = id;
    this.stanzaId = stanzaId;
  }

  @Override
  @Nullable
  public String id() {
    return id;
  }

  @Override
  @Nullable
  public String stanzaId() {
    return stanzaId;
  }
}
