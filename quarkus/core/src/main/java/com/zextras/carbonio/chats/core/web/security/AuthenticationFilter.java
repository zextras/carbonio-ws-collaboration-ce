// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.ws.core.security.SecurityAttributes;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Optional;
import java.util.UUID;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Bridges the Quarkus {@link SecurityIdentity} (built once per request by {@code ZmAuthMechanism}
 * and {@code UserManagementIdentityProvider}, the single User Management call) to the CE {@link
 * UserPrincipal} read by the CE API services through {@code SecurityContext}. Keeps the CE
 * semantics: invalid token or queue id → 401, inactive user or missing WSC feature → 403, no
 * cookie → anonymous (the CE anonymous principal had no name, so {@code getUserPrincipal()} was
 * {@code null}; here the default security context is left in place).
 */
@ApplicationScoped
public class AuthenticationFilter {

  public static final String QUEUE_ID_HEADER = "queue-id";

  private final CurrentIdentityAssociation identityAssociation;

  @Inject
  public AuthenticationFilter(CurrentIdentityAssociation identityAssociation) {
    this.identityAssociation = identityAssociation;
  }

  @ServerRequestFilter(preMatching = true, priority = Priorities.AUTHENTICATION)
  public Uni<Void> filter(ContainerRequestContext requestContext) {
    UUID queueId;
    try {
      queueId = parseQueueId(requestContext.getHeaderString(QUEUE_ID_HEADER));
    } catch (UnauthorizedException e) {
      return Uni.createFrom().failure(e);
    }
    return identityAssociation
        .getDeferredIdentity()
        .onFailure(AuthenticationFailedException.class)
        .transform(e -> new UnauthorizedException())
        .invoke(
            identity ->
                toPrincipal(identity, queueId)
                    .ifPresent(
                        principal ->
                            requestContext.setSecurityContext(
                                SecurityContextImpl.create(principal))))
        .replaceWithVoid();
  }

  public static UUID parseQueueId(String rawQueueId) {
    try {
      return Optional.ofNullable(rawQueueId).map(UUID::fromString).orElse(null);
    } catch (IllegalArgumentException e) {
      throw new UnauthorizedException("Invalid queue id");
    }
  }

  /**
   * The CE principal for an identity, empty when anonymous.
   *
   * @throws ForbiddenException when the user is inactive or lacks the WSC feature
   */
  public static Optional<UserPrincipal> toPrincipal(SecurityIdentity identity, UUID queueId) {
    if (identity == null || identity.isAnonymous()) {
      return Optional.empty();
    }
    String denied = identity.getAttribute(SecurityAttributes.DENIED_REASON);
    if (denied != null) {
      throw new ForbiddenException(denied);
    }
    com.zextras.carbonio.ws.plugin.api.UserPrincipal user =
        identity.getAttribute(SecurityAttributes.USER);
    if (user == null) {
      return Optional.empty();
    }
    return Optional.of(
        UserPrincipal.create(user.id())
            .queueId(queueId)
            .userType(UserType.from(user.type().name()))
            .email(user.email())
            .name(user.name())
            .authToken(user.authToken())
            .carbonioAttributes(new java.util.HashMap<>(user.attributes())));
  }
}
