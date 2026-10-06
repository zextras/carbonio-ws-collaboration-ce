// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import com.zextras.carbonio.ws.plugin.api.CurrentUser;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.UUID;

@RequestScoped
public class CurrentUserImpl implements CurrentUser {

  @Inject SecurityIdentity identity;
  @Inject HttpServerRequest request;

  @Override
  public UserPrincipal get() {
    UserPrincipal user = identity.isAnonymous() ? null : identity.getAttribute(SecurityAttributes.USER);
    if (user == null) {
      throw new NotAuthorizedException("Unauthorized");
    }
    String rawQueueId = request.getHeader("queue-id");
    if (rawQueueId == null) {
      return user;
    }
    try {
      UUID queueId = UUID.fromString(rawQueueId);
      return new UserPrincipal(
          user.id(), user.type(), user.email(), user.name(), user.authToken(), queueId, user.attributes());
    } catch (IllegalArgumentException e) {
      throw new NotAuthorizedException("Invalid queue id");
    }
  }
}
