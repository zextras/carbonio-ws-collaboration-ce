// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.UserType;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class UserManagementIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

  static final String WSC_FEATURE = "carbonioFeatureWscEnabled";

  @Inject AuthenticationService authenticationService;

  @Override
  public Class<TokenAuthenticationRequest> getRequestType() {
    return TokenAuthenticationRequest.class;
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      TokenAuthenticationRequest request, AuthenticationRequestContext context) {
    return context.runBlocking(() -> toIdentity(request.getToken().getToken()));
  }

  SecurityIdentity toIdentity(String token) {
    // The one User Management call per request. CE semantics: only a real 401 is "not
    // authenticated"; any other failure surfaces as the CE AuthenticationException (dependency).
    MyselfDto myself =
        authenticationService
            .getUserMyself(token)
            .orElseThrow(() -> new AuthenticationFailedException("Invalid token"));
    UserInfoDto info = myself.getInfo();
    UserPrincipal principal =
        new UserPrincipal(
            UUID.fromString(info.getUserId()),
            UserType.from(info.getType()),
            info.getEmail(),
            info.getFullName(),
            token,
            null,
            myself.getCapabilities());
    QuarkusSecurityIdentity.Builder builder =
        QuarkusSecurityIdentity.builder()
            .setPrincipal(new QuarkusPrincipal(info.getUserId()))
            .addRole("user")
            .addAttribute(SecurityAttributes.USER, principal);
    String denied = deniedReason(myself);
    if (denied != null) {
      builder.addAttribute(SecurityAttributes.DENIED_REASON, denied);
    }
    return builder.build();
  }

  private static String deniedReason(MyselfDto myself) {
    if (!"active".equalsIgnoreCase(myself.getInfo().getStatus())) {
      return "User is not active";
    }
    if (myself.getFeatures() == null || !myself.getFeatures().contains(WSC_FEATURE)) {
      return "WSC feature not enabled for user";
    }
    return null;
  }
}
