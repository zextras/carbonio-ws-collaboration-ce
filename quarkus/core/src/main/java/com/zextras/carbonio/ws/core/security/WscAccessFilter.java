// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/** 403 for authenticated users that are inactive or lack the WSC feature. */
public class WscAccessFilter {

  @Inject SecurityIdentity identity;

  @ServerRequestFilter(priority = Priorities.AUTHORIZATION)
  public Optional<Response> refuseDeniedUsers() {
    if (identity.isAnonymous()) {
      return Optional.empty();
    }
    String reason = identity.getAttribute(SecurityAttributes.DENIED_REASON);
    return reason == null
        ? Optional.empty()
        : Optional.of(Response.status(403).type(MediaType.TEXT_PLAIN).entity(reason).build());
  }
}
