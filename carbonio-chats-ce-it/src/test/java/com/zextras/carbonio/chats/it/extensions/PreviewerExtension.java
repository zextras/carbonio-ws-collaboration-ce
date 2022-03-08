package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.PreviewerMockServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.MediaType;
import org.mockserver.netty.MockServer;

public class PreviewerExtension implements AfterAllCallback, BeforeAllCallback, ParameterResolver {

  private static final String    SERVER_HOST          = "localhost";
  private static final int       SERVER_PORT          = 7894;
  private final static Namespace EXTENSION_NAMESPACE  = Namespace.create(PreviewerExtension.class);
  private final static String    CLIENT_PREVIEW_ENTRY = "client";
  private final static String    SERVER_PREVIEW_ENTRY = "server";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting Previewer MockServer...");
    MockServer mockServer = new MockServer(SERVER_PORT);
    context.getStore(EXTENSION_NAMESPACE).put(SERVER_PREVIEW_ENTRY, mockServer);

    ChatsLogger.debug("Starting Previewer mock...");
    MockServerClient client = new PreviewerMockServer(SERVER_HOST, SERVER_PORT);
    mockResponses(client);
    context.getStore(EXTENSION_NAMESPACE).put(CLIENT_PREVIEW_ENTRY, client);

    InMemoryConfigStore.set(ConfigValue.PREVIEWER_HOST, SERVER_HOST);
    InMemoryConfigStore.set(ConfigValue.PREVIEWER_PORT, Integer.toString(SERVER_PORT));
    ChatsLogger.debug(
      "Previewer extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(CLIENT_PREVIEW_ENTRY))
      .map(objectMockClient -> (MockServerClient) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping Previewer mock...");
        client.stop(true);
      });
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(SERVER_PREVIEW_ENTRY))
      .map(objectMockClient -> (MockServer) objectMockClient)
      .ifPresent(client -> {
        ChatsLogger.debug("Stopping Previewer Mockserver...");
        client.stop();
      });
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(PreviewerMockServer.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(PreviewerMockServer.class)) {
      return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(CLIENT_PREVIEW_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  private void mockResponses(MockServerClient client) throws IOException {
    mockGetPreview(client, MockedFiles.get(MockedFileType.SNOOPY_PREVIEW));
  }

  private void mockGetPreview(MockServerClient client, FileMock fileMock) throws IOException {
    BasicHttpEntity entity = new BasicHttpEntity();
    entity.setContent(new ByteArrayInputStream(fileMock.getFileBytes()));
    client.when(
      request()
        .withMethod("GET")
        .withPath("/preview/image/{fileId}/{version}/{area}")
        .withPathParameter("fileId", fileMock.getId())
        .withPathParameter("version", "1")
        .withPathParameter("area", ChatsConstant.PREVIEW_AREA)
        .withQueryStringParameter(param("service_type", "chats"))
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(
          BinaryBody.binary(fileMock.getFileBytes())
        )
        .withContentType(MediaType.JPEG)
    );

  }
}
