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
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.PreviewerMockServer;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.ClearType;
import org.mockserver.model.MediaType;

public class PreviewerExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 7894;
  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(PreviewerExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "preview_client";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting Previewer mock...");
      PreviewerMockServer client = new PreviewerMockServer(SERVER_PORT);
      mockResponses(client);
      InMemoryConfigStore.set(ConfigValue.PREVIEWER_HOST, SERVER_HOST);
      InMemoryConfigStore.set(ConfigValue.PREVIEWER_PORT, Integer.toString(SERVER_PORT));
      return client;
    }, PreviewerMockServer.class);
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
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (PreviewerMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
      );
  }

  private void mockResponses(MockServerClient client) {
    mockIsAlive(client);
    mockGetPreview(client, MockedFiles.getPreview(MockedFileType.SNOOPY_PREVIEW));
  }

  private void mockGetPreview(MockServerClient client, FileMock fileMock) {
    try {
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void mockIsAlive(MockServerClient client) {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/health/ready/")
    ).respond(
      response()
        .withStatusCode(200)
    );
  }
}
