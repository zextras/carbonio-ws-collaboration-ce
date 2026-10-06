// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

public final class SecurityAttributes {
  /** {@link com.zextras.carbonio.ws.plugin.api.UserPrincipal} of the authenticated user. */
  public static final String USER = "ws.user";

  /** Present when the token is valid but the user must be refused with 403. */
  public static final String DENIED_REASON = "ws.denied-reason";

  private SecurityAttributes() {}
}
