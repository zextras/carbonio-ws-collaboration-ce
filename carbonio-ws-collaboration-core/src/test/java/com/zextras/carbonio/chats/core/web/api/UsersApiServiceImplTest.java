// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UsersApiServiceImplTest {

  private final UsersApiService usersApiService;
  private final UserService userService;
  private final CapabilityService capabilityService;
  private final SecurityContext securityContext;

  public UsersApiServiceImplTest() {
    this.userService = mock(UserService.class);
    this.capabilityService = mock(CapabilityService.class);
    this.securityContext = mock(SecurityContext.class);
    this.usersApiService = new UsersApiServiceImpl(userService, capabilityService);
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;

  private UserPrincipal user1;

  private String snoopyFileName;

  @BeforeEach
  void init() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();

    user1 = UserPrincipal.create(user1Id);

    snoopyFileName = StringFormatUtils.encodeToUtf8("snoopy-image");
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(userService, capabilityService);
    reset(userService, capabilityService, securityContext);
  }

  @Nested
  @DisplayName("Get user tests")
  class GetUserTest {

    @Test
    @DisplayName("Get user info with an authenticated user")
    void getUser_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = usersApiService.getUser(user1Id, securityContext);

      verify(userService, times(1)).getUserById(user1Id, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get user info with a non-valid user")
    void getUser_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> usersApiService.getUser(user1Id, securityContext));
    }
  }

  @Nested
  @DisplayName("Get users tests")
  class GetUsersTest {

    @Test
    @DisplayName("Get users info with an authenticated user")
    void getUsers_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = usersApiService.getUsers(List.of(user1Id, user2Id), securityContext);

      verify(userService, times(1)).getUsersByIds(List.of(user1Id, user2Id), user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get users info with an authenticated user with an empty list")
    void getUsers_testAuthenticatedUser_emptyList() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = usersApiService.getUsers(List.of(), securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get users info with an authenticated user with a list of 11 ids")
    void getUsers_testAuthenticatedUser_tooManyUsers() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          usersApiService.getUsers(
              List.of(
                  user1Id,
                  user1Id,
                  user2Id,
                  user3Id,
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  UUID.randomUUID()),
              securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get users info with a non-valid user")
    void getUsers_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      List<UUID> users = List.of(user1Id, user2Id);
      assertThrows(
          UnauthorizedException.class, () -> usersApiService.getUsers(users, securityContext));
    }
  }

  @Nested
  @DisplayName("Get capabilities tests")
  class GetCapabilitiesTest {

    @Test
    @DisplayName("Get capabilities with an authenticated user")
    void getCapabilities_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = usersApiService.getCapabilities(securityContext);

      verify(capabilityService, times(1)).getCapabilities(user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get capabilities with a non-valid user")
    void getCapabilities_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> usersApiService.getCapabilities(securityContext));
    }
  }
}
