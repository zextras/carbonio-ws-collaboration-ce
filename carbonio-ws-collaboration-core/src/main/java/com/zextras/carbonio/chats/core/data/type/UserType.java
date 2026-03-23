// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.type;

import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;

public enum UserType {
  INTERNAL,
  GUEST;

  public static UserType from(UserTypeProto umType) {
    return switch (umType) {
      case GUEST -> GUEST;
      case INTERNAL -> INTERNAL;
      default -> INTERNAL;
    };
  }
}
