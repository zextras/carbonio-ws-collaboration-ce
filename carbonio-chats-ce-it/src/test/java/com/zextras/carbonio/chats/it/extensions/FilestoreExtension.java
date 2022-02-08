package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.BinaryBody;

public class FilestoreExtension implements AfterAllCallback, BeforeAllCallback {

  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 6794;
  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(FilestoreExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "client";
  private final static String    SERVER_STORE_ENTRY  = "server";
  private final static String    UUID_REGEX          = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting Storages MockServer...");
    ClientAndServer mockServer = ClientAndServer.startClientAndServer(SERVER_PORT);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_STORE_ENTRY, mockServer);

    ChatsLogger.debug("Starting Storages mock...");
    MockServerClient client = new MockServerClient(SERVER_HOST, SERVER_PORT);
    mockResponses(client);
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_STORE_ENTRY, client);

    InMemoryConfigStore.set("FILESTORE_URL", String.format("http://%s:%d", SERVER_HOST, SERVER_PORT));
    ChatsLogger.debug("Storage extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
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

  private void mockResponses(MockServerClient client) throws IOException {
    mockDownloadResponse(client);
    mockHealthLiveResponse(client);
  }

  private void mockDownloadResponse(MockServerClient client) throws IOException {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/download")
        .withQueryStringParameter(param("node", UUID_REGEX))
        .withQueryStringParameter(param("type", "chats"))
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(
          BinaryBody.binary(getClass().getResourceAsStream("/images/carbonio-for-public-administration.jpg").readAllBytes())
        )
    );
  }

  private void mockHealthLiveResponse(MockServerClient client) {
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
