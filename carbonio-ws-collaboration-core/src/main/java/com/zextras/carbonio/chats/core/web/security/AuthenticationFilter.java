// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Provider
@PreMatching
@Singleton
public class AuthenticationFilter implements ContainerRequestFilter {

  private final AuthenticationService authenticationService;

  @Inject
  public AuthenticationFilter(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    UUID queueId;
    try {
      queueId =
          Optional.ofNullable(requestContext.getHeaderString("queue-id"))
              .map(UUID::fromString)
              .orElse(null);
    } catch (IllegalArgumentException e) {
      throw new UnauthorizedException("Invalid queue id");
    }
    Optional.ofNullable(requestContext.getCookies().get(AuthenticationMethod.ZM_AUTH_TOKEN.name()))
        .map(Cookie::getValue)
        .ifPresentOrElse(
            token -> {
              // If the user token is invalid, we won't authenticate him/her as anonymous
              MyselfDto userMyself =
                  authenticationService
                      .getUserMyself(token)
                      .orElseThrow(UnauthorizedException::new);

              // Check if user status is ACTIVE
              if (!userMyself.getInfo().getStatus().equalsIgnoreCase("active")) {
                throw new ForbiddenException("User is not active");
              }

              // Check if carbonioFeatureWscEnabled is in features list
              boolean wscEnabled =
                  userMyself
                      .getFeatures()
                      .contains(CarbonioAttribute.FEATURE_WSC_ENABLED.getValue());
              if (!wscEnabled) {
                throw new ForbiddenException("WSC feature not enabled for user");
              }
              Map<String, String> capabilities = userMyself.getCapabilities();
              requestContext.setSecurityContext(
                  SecurityContextImpl.create(
                      UserPrincipal.create(userMyself.getInfo().getUserId())
                          .queueId(queueId)
                          .userType(UserType.from(userMyself.getInfo().getType()))
                          .email(userMyself.getInfo().getEmail())
                          .name(userMyself.getInfo().getFullName())
                          .authToken(token)
                          .carbonioAttributes(capabilities)));
            },
            () -> // The user didn't specify any authorization, we're logging him/her as anonymous
                // (useful for healthchecks)
                requestContext.setSecurityContext(
                    SecurityContextImpl.create(UserPrincipal.create((UUID) null).authToken(null))));
  }
}
