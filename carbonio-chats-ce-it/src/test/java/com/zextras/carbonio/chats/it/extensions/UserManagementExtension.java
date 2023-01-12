// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.JsonBody;

public class UserManagementExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 7854;
  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(UserManagementExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "user_management_client";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting User Management mock...");
      UserManagementMockServer client = new UserManagementMockServer(SERVER_PORT);
      mockResponses(client);
      InMemoryConfigStore.set(ConfigName.USER_MANAGEMENT_HOST, SERVER_HOST);
      InMemoryConfigStore.set(ConfigName.USER_MANAGEMENT_PORT, Integer.toString(SERVER_PORT));
      return client;
    }, UserManagementMockServer.class);
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(UserManagementMockServer.class);
  }

  @Override
  public Object resolveParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(UserManagementMockServer.class)) {
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) {
    mockHealthCheck(client);
    for (MockUserProfile mockAccount : MockedAccount.getAccounts()) {
      mockValidateUserToken(client, new UserId(mockAccount.getId()), mockAccount.getToken());
      UserInfo userInfo = new UserInfo(mockAccount.getId(), mockAccount.getEmail(), mockAccount.getName(),
        mockAccount.getDomain());
      mockGetUserByUUID(client, userInfo);
      mockGetUserByEmail(client, userInfo);
    }
  }

  private void mockHealthCheck(MockServerClient client) {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/health/")
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockValidateUserToken(MockServerClient client, UserId userId, String carbonioUserToken) {
    client.when(
      request()
        .withMethod("GET")
        .withPath(String.format("/auth/token/%s", carbonioUserToken))
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(JsonBody.json(userId))
    );
  }

  private void mockGetUserByUUID(MockServerClient client, UserInfo userInfo) {
    client.when(
      request()
        .withMethod("GET")
        .withPath(String.format("/users/id/%s", userInfo.getId()))
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(JsonBody.json(userInfo))
    );
  }

  private void mockGetUserByEmail(MockServerClient client, UserInfo userInfo) {
    client.when(
      request()
        .withMethod("GET")
        .withPath(String.format("/users/email/%s", userInfo.getEmail()))
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(JsonBody.json(userInfo))
    );
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (UserManagementMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
      );
  }
}
