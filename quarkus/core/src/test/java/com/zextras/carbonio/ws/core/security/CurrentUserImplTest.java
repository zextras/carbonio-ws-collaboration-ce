// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.UserType;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.ForbiddenException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurrentUserImplTest {

  private static CurrentUserImpl with(QuarkusSecurityIdentity.Builder builder) {
    CurrentUserImpl current = new CurrentUserImpl();
    current.identity = builder.build();
    current.request = mock(HttpServerRequest.class);
    return current;
  }

  private static QuarkusSecurityIdentity.Builder authenticated() {
    UUID id = UUID.randomUUID();
    UserPrincipal user =
        new UserPrincipal(id, UserType.INTERNAL, "a@b.c", "A", "t", null, Map.of());
    return QuarkusSecurityIdentity.builder()
        .setPrincipal(new QuarkusPrincipal(id.toString()))
        .addAttribute(SecurityAttributes.USER, user);
  }

  @Test
  void deniedUserIsForbidden() {
    CurrentUserImpl current =
        with(authenticated().addAttribute(SecurityAttributes.DENIED_REASON, "User is not active"));
    assertThatThrownBy(current::get)
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("User is not active");
  }

  @Test
  void allowedUserIsReturned() {
    assertThat(with(authenticated()).get().type()).isEqualTo(UserType.INTERNAL);
  }
}
