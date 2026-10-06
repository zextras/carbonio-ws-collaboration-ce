// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.type;

public enum UserType {
  INTERNAL,
  GUEST;

  /**
   * Maps the plain-string {@code type} field returned by the User Management REST SDK (e.g. {@code
   * UserInfoDto.getType()}) to a {@link UserType}. Unknown/unexpected values default to {@link
   * #INTERNAL}, matching the previous gRPC enum mapping's default branch.
   */
  public static UserType from(String umType) {
    return "GUEST".equalsIgnoreCase(umType) ? GUEST : INTERNAL;
  }
}
