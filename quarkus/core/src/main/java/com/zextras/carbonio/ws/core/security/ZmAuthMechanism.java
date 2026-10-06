// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

/** Reads the Carbonio {@code ZM_AUTH_TOKEN} cookie; no cookie means anonymous. */
@ApplicationScoped
public class ZmAuthMechanism implements HttpAuthenticationMechanism {

  static final String COOKIE = "ZM_AUTH_TOKEN";

  @Override
  public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager manager) {
    Cookie cookie = context.request().getCookie(COOKIE);
    if (cookie == null || cookie.getValue().isBlank()) {
      return Uni.createFrom().nullItem();
    }
    return manager.authenticate(
        new TokenAuthenticationRequest(new TokenCredential(cookie.getValue(), COOKIE)));
  }

  @Override
  public Uni<ChallengeData> getChallenge(RoutingContext context) {
    return Uni.createFrom().item(new ChallengeData(401, null, null));
  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
    return Set.of(TokenAuthenticationRequest.class);
  }
}
