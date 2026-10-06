// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import java.util.Map;
import java.util.UUID;

public record UserPrincipal(
    UUID id,
    UserType type,
    String email,
    String name,
    String authToken,
    UUID queueId,
    Map<String, String> attributes) {

  public UserPrincipal {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

  public boolean isGuest() {
    return type == UserType.GUEST;
  }

  public boolean hasAttribute(String name) {
    return "true".equalsIgnoreCase(attributes.get(name));
  }
}
