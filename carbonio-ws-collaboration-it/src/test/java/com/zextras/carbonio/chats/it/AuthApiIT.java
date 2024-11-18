// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.AuthApi;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.chats.model.TokenDto;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class AuthApiIT {

  private final UserManagementMockServer userManagementMockServer;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;

  public AuthApiIT(
      AuthApi authApi,
      UserManagementMockServer userManagementMockServer,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper) {
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.dispatcher.getRegistry().addSingletonResource(authApi);
  }

  @Test
  @DisplayName("Correctly gets the authenticated token")
  void getToken_testOk() throws Exception {
    String url = "/auth/token";
    MockUserProfile account = MockedAccount.getAccounts().get(0);
    MockHttpResponse response = dispatcher.get(url, account.getToken());
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    TokenDto token = objectMapper.readValue(response.getContentAsString(), TokenDto.class);
    assertEquals(account.getToken(), token.getZmToken());
  }

  @Test
  @DisplayName("If there isn't an authenticated token return a status code 401")
  void getToken_testErrorUnauthenticatedUser() throws Exception {
    String url = "/auth/token";
    MockHttpResponse response = dispatcher.get(url, null);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }
}
