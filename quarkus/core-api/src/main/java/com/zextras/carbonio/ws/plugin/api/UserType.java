// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

public enum UserType {
  INTERNAL,
  GUEST;

  public static UserType from(String type) {
    return "GUEST".equalsIgnoreCase(type) ? GUEST : INTERNAL;
  }
}
