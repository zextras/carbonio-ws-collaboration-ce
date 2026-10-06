// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.testsupport;

import java.util.UUID;

public final class TestUsers {

  public static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final UUID MEMBER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  public static final UUID GUEST = UUID.fromString("33333333-3333-3333-3333-333333333333");
  public static final UUID OUTSIDER = UUID.fromString("44444444-4444-4444-4444-444444444444");

  private TestUsers() {}

  public static String token(UUID userId) {
    return "token-" + userId;
  }
}
