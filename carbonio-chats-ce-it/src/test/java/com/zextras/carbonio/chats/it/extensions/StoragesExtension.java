package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.storages.internal.pojo.Query;
import java.io.IOException;
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

public class StoragesExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(StoragesExtension.class);
  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 6794;
  private final static String    CLIENT_STORE_ENTRY  = "storages_client";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting Storages mock...");
      StorageMockServer client = new StorageMockServer(SERVER_PORT);
      mockResponses(client);
      InMemoryConfigStore.set(ConfigName.STORAGES_HOST, SERVER_HOST);
      InMemoryConfigStore.set(ConfigName.STORAGES_PORT, Integer.toString(SERVER_PORT));
      return client;
    }, StorageMockServer.class);
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
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) {
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

  private void mockDownload(MockServerClient client, FileMock fileMock) {
    try {
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (StorageMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
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
