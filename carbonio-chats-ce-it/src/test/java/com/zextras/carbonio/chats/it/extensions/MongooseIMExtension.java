package com.zextras.carbonio.chats.it.extensions;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
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
import org.mockserver.mock.OpenAPIExpectation;
import org.mockserver.netty.MockServer;

public class MongooseIMExtension implements AfterAllCallback, BeforeAllCallback, ParameterResolver {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(MongooseIMExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "client";
  private final static String    SERVER_STORE_ENTRY  = "server";
  private final static int       PORT                = 12345;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting MongooseIM Mockserver...");
    MockServer mockServer = new MockServer(PORT);
    ChatsLogger.debug("Starting MongooseIM client mock...");
    MockServerClient mockClient = new MongooseImMockServer("localhost", PORT);
    mockClient.upsert(
      OpenAPIExpectation.openAPIExpectation("openapi/mongooseim-client-api.yaml")
    );
    mockClient.upsert(
      OpenAPIExpectation.openAPIExpectation("openapi/mongooseim-admin-api.yaml")
    );
    InMemoryConfigStore.set("MONGOOSEIM_CLIENT_REST_BASE_URL", httpUrlFromMockClient(mockClient));
    InMemoryConfigStore.set("MONGOOSEIM_ADMIN_REST_BASE_URL", httpUrlFromMockClient(mockClient));
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_STORE_ENTRY, mockClient);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_STORE_ENTRY, mockServer);
    ChatsLogger.debug(
      "Mongoose extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(objectMockClient -> (MongooseImMockServer) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping MongooseIM client mock...");
        client.stop(true);
      });
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(SERVER_STORE_ENTRY))
      .map(objectMockClient -> (MockServer) objectMockClient)
      .ifPresent(server -> {
        ChatsLogger.debug("Stopping MongooseIM Mockserver...");
        server.stop();
      });
  }

  private String httpUrlFromMockClient(MockServerClient mockServerClient) {
    StringBuilder stringBuilder = new StringBuilder();
    if (mockServerClient.isSecure()) {
      stringBuilder.append("https://");
    } else {
      stringBuilder.append("http://");
    }
    stringBuilder.append(mockServerClient.remoteAddress().getHostString());
    stringBuilder.append(":");
    stringBuilder.append(mockServerClient.remoteAddress().getPort());
    stringBuilder.append("/");
    stringBuilder.append(mockServerClient.contextPath());
    return stringBuilder.toString();
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(MongooseImMockServer.class);
  }

  @Override
  public Object resolveParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(MongooseImMockServer.class)) {
      return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }
}
