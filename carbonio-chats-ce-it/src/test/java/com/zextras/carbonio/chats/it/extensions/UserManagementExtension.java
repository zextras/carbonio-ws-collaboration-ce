package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedAccount.MockAccount;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.netty.MockServer;

public class UserManagementExtension implements AfterAllCallback, BeforeAllCallback, ParameterResolver {

  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 7854;
  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(UserManagementExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "client";
  private final static String    SERVER_STORE_ENTRY  = "server";

  @Override
  public void beforeAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting User Management MockServer...");
    MockServer mockServer = new MockServer(SERVER_PORT);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_STORE_ENTRY, mockServer);

    ChatsLogger.debug("Starting User Management mock...");
    MockServerClient client = new UserManagementMockServer(SERVER_HOST, SERVER_PORT);
    mockResponses(client);
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_STORE_ENTRY, client);

    InMemoryConfigStore.set("USER_MANAGEMENT_URL", String.format("http://%s:%d", SERVER_HOST, SERVER_PORT));
    ChatsLogger.debug("User Management extension startup took " + TimeUtils.durationToString(
      Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(objectMockClient -> (MockServerClient) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping User Management mock...");
        client.stop(true);
      });
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(SERVER_STORE_ENTRY))
      .map(objectMockClient -> (MockServer) objectMockClient)
      .ifPresent(server -> {
        ChatsLogger.debug("Stopping User Management Mockserver...");
        server.stop();
      });
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
      return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) {
    mockHealthCheck(client);
    for (MockAccount mockAccount : MockedAccount.getAccounts()) {
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
}
