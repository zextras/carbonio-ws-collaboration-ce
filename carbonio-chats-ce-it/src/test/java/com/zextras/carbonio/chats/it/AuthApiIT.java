package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.AuthApi;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.model.TokenDto;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class AuthApiIT {

  private final AuthApi                   authApi;
  private final UserManagementMockServer  userManagementMockServer;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;

  public AuthApiIT(
    AuthApi authApi, UserManagementMockServer userManagementMockServer,
    ResteasyRequestDispatcher dispatcher,
    ObjectMapper objectMapper
  ) {
    this.authApi = authApi;
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.dispatcher.getRegistry().addSingletonResource(authApi);
  }

  @Test
  public void getToken_testOk() throws Exception {
    String url = "/auth/token";
    MockUserProfile account = MockedAccount.getAccounts().get(0);
    MockHttpResponse response = dispatcher.get(url, account.getToken());
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    TokenDto token = objectMapper.readValue(response.getContentAsString(), TokenDto.class);
    assertEquals(account.getToken(), token.getZmToken());
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
  }

  @Test
  public void getToken_testErrorUnauthenticatedUser() throws Exception {
    String url = "/auth/token";
    MockUserProfile account = MockedAccount.getAccounts().get(0);
    MockHttpResponse response = dispatcher.get(url, null);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }
}
