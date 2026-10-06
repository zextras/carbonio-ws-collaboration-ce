// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

/** Request-scoped access to the authenticated user. */
public interface CurrentUser {
  /** @throws jakarta.ws.rs.NotAuthorizedException when the request is anonymous */
  UserPrincipal get();
}
