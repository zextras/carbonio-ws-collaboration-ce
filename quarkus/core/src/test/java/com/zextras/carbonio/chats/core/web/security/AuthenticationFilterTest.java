// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.ws.core.security.SecurityAttributes;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationFilterTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private static QuarkusSecurityIdentity.Builder authenticated() {
    return QuarkusSecurityIdentity.builder()
        .setPrincipal(new QuarkusPrincipal(USER_ID.toString()))
        .addAttribute(
            SecurityAttributes.USER,
            new com.zextras.carbonio.ws.plugin.api.UserPrincipal(
                USER_ID,
                com.zextras.carbonio.ws.plugin.api.UserType.GUEST,
                "a@b.c",
                "A B",
                "token",
                null,
                Map.of("cap", "true")));
  }

  @Test
  void anonymousIdentityGivesNoPrincipal() {
    assertThat(
            AuthenticationFilter.toPrincipal(
                QuarkusSecurityIdentity.builder().setAnonymous(true).build(), null))
        .isEmpty();
  }

  @Test
  void authenticatedIdentityGivesTheCePrincipal() {
    UUID queueId = UUID.randomUUID();
    UserPrincipal principal =
        AuthenticationFilter.toPrincipal(authenticated().build(), queueId).orElseThrow();
    assertThat(principal.getUUID()).isEqualTo(USER_ID);
    assertThat(principal.getQueueId()).isEqualTo(queueId);
    assertThat(principal.getUserType()).isEqualTo(UserType.GUEST);
    assertThat(principal.getEmail()).isEqualTo("a@b.c");
    assertThat(principal.getName()).isEqualTo("A B");
    assertThat(principal.getAuthToken()).contains("token");
  }

  @Test
  void deniedIdentityIsForbidden() {
    assertThatThrownBy(
            () ->
                AuthenticationFilter.toPrincipal(
                    authenticated().addAttribute(SecurityAttributes.DENIED_REASON, "no").build(),
                    null))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void invalidQueueIdIsUnauthorized() {
    assertThat(AuthenticationFilter.parseQueueId(null)).isNull();
    assertThatThrownBy(() -> AuthenticationFilter.parseQueueId("nope"))
        .isInstanceOf(UnauthorizedException.class);
  }
}
