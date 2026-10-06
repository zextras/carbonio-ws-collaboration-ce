// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.ws.core.security.SecurityAttributes;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Handshake-time authentication of {@code /events}, replacing CE's servlet filter. quarkus-websockets
 * upgrades in a Vert.x filter (priority 101) that runs before the HTTP security routes, so this
 * filter runs first and authenticates the upgrade request through the same identity provider as
 * REST (ZM_AUTH_TOKEN, one User Management call): no or invalid token → 401, inactive user or
 * missing WSC feature → 403, User Management failure → 500. The identity is set on the routing
 * context, so the websocket session principal carries it.
 */
@ApplicationScoped
public class EventsWebSocketAuthenticationFilter {

  static final int PRIORITY = 200;

  private final IdentityProviderManager identityProviderManager;

  @Inject
  public EventsWebSocketAuthenticationFilter(IdentityProviderManager identityProviderManager) {
    this.identityProviderManager = identityProviderManager;
  }

  void register(@Observes Filters filters) {
    filters.register(this::filter, PRIORITY);
  }

  void filter(RoutingContext context) {
    if (!EventsWebSocketApplicationConfig.PATH.equals(context.normalizedPath())) {
      context.next();
      return;
    }
    Cookie cookie = context.request().getCookie(AuthenticationMethod.ZM_AUTH_TOKEN.name());
    if (cookie == null || cookie.getValue().isBlank()) {
      context.response().setStatusCode(401).end();
      return;
    }
    identityProviderManager
        .authenticate(
            new TokenAuthenticationRequest(
                new TokenCredential(cookie.getValue(), AuthenticationMethod.ZM_AUTH_TOKEN.name())))
        .subscribe()
        .with(
            identity -> {
              if (refused(identity)) {
                context.response().setStatusCode(403).end();
              } else {
                context.setUser(new QuarkusHttpUser(identity));
                context.next();
              }
            },
            failure -> {
              if (failure instanceof AuthenticationFailedException) {
                context.response().setStatusCode(401).end();
              } else {
                ChatsLogger.warn("Websocket authentication failed: " + failure);
                context.response().setStatusCode(500).end();
              }
            });
  }

  private static boolean refused(SecurityIdentity identity) {
    return identity == null
        || identity.isAnonymous()
        || identity.getAttribute(SecurityAttributes.DENIED_REASON) != null
        || identity.getAttribute(SecurityAttributes.USER) == null;
  }
}
