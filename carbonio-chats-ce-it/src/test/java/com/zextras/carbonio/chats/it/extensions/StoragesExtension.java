package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import java.io.IOException;
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
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.BinaryBody;

public class StoragesExtension implements AfterAllCallback, BeforeAllCallback, ParameterResolver {

  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 6794;
  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(StoragesExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "client";
  private final static String    SERVER_STORE_ENTRY  = "server";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting Storages MockServer...");
    ClientAndServer mockServer = ClientAndServer.startClientAndServer(SERVER_PORT);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_STORE_ENTRY, mockServer);

    ChatsLogger.debug("Starting Storages mock...");
    MockServerClient client = new StorageMockServer(SERVER_HOST, SERVER_PORT);
    mockResponses(client);
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_STORE_ENTRY, client);

    InMemoryConfigStore.set("STORAGES_URL", String.format("http://%s:%d", SERVER_HOST, SERVER_PORT));
    ChatsLogger.debug("Storage extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(objectMockClient -> (MockServerClient) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping Storages mock...");
        client.stop(true);
      });
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(SERVER_STORE_ENTRY))
      .map(objectMockClient -> (ClientAndServer) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping MongooseIM Mockserver...");
        client.stop(true);
      });
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(StorageMockServer.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(StorageMockServer.class)) {
      return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) throws IOException {
    for (FileMock mockedFIle : MockedFiles.getMockedFiles()) {
      mockDownload(client, mockedFIle);
      mockDelete(client, mockedFIle.getId());
    }
    mockHealthLive(client);
  }

  private void mockDownload(MockServerClient client, FileMock fileMock) throws IOException {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/download")
        .withQueryStringParameter(param("node", fileMock.getId()))
        .withQueryStringParameter(param("type", "chats"))
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(
          BinaryBody.binary(fileMock.getFileBytes())
        )
    );
  }

  private void mockDelete(MockServerClient client, String fileId) {
    client.when(
      request()
        .withMethod("DELETE")
        .withQueryStringParameter(param("node", fileId))
        .withQueryStringParameter(param("type", "chats"))
    ).respond(
      response()
        .withStatusCode(200)
    );
  }

  private void mockHealthLive(MockServerClient client) {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/health/live")
    ).respond(
      response()
        .withStatusCode(204)
    );
  }
}
