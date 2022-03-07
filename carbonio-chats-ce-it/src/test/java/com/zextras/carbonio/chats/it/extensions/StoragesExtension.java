package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.storages.internal.pojo.Query;
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
import org.mockserver.model.JsonBody;
import org.mockserver.netty.MockServer;

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
    MockServer mockServer = new MockServer(SERVER_PORT);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_STORE_ENTRY, mockServer);

    ChatsLogger.debug("Starting Storages mock...");
    MockServerClient client = new StorageMockServer(SERVER_HOST, SERVER_PORT);
    mockResponses(client);
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_STORE_ENTRY, client);
    InMemoryConfigStore.set(ConfigValue.STORAGES_HOST, SERVER_HOST);
    InMemoryConfigStore.set(ConfigValue.STORAGES_PORT, Integer.toString(SERVER_PORT));
    ChatsLogger.debug(
      "Storage extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) {
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
      .map(objectMockClient -> (MockServer) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping MongooseIM Mockserver...");
        client.stop();
      });
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(StorageMockServer.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(StorageMockServer.class)) {
      return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) throws IOException {
    for (FileMock mockedFile : MockedFiles.getMockedFiles()) {
      mockDownload(client, mockedFile);
      mockUpload(
        client,
        mockedFile,
        new UploadResponse().digest("").digestAlgorithm("").size(mockedFile.getSize())
      );
      mockDelete(client, mockedFile.getId());
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
          binary(fileMock.getFileBytes())
        )
    );
  }

  private void mockUpload(MockServerClient client, FileMock fileMock, UploadResponse response) {
    client.when(
      request()
        .withMethod("PUT")
        .withPath("/upload")
        .withQueryStringParameter(param("node", fileMock.getId()))
        .withQueryStringParameter(param("type", "chats"))
    ).respond(
      response()
        .withStatusCode(201)
        .withBody(JsonBody.json(response))
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

  static private class UploadResponse implements com.zextras.filestore.api.UploadResponse {

    private Query  query;
    private String resource;
    private String digest;
    private String digestAlgorithm;
    private long   size;

    public Query getQuery() {
      return query;
    }

    public UploadResponse query(Query query) {
      this.query = query;
      return this;
    }

    public String getResource() {
      return resource;
    }

    public UploadResponse resource(String resource) {
      this.resource = resource;
      return this;
    }

    public String getDigest() {
      return digest;
    }

    public UploadResponse digest(String digest) {
      this.digest = digest;
      return this;
    }

    public String getDigestAlgorithm() {
      return digestAlgorithm;
    }

    public UploadResponse digestAlgorithm(String digestAlgorithm) {
      this.digestAlgorithm = digestAlgorithm;
      return this;
    }

    public long getSize() {
      return size;
    }

    public UploadResponse size(long size) {
      this.size = size;
      return this;
    }
  }
}
