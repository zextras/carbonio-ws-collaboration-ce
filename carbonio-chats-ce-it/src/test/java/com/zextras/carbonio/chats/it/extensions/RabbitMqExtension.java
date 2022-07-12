package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.RabbitMqMockServer;
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

public class RabbitMqExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(RabbitMqExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "rabbitmq_client";
  private final static int       PORT                = 12346;
  private static final String    HOST                = "localhost";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting RabbitMq client mock...");
      RabbitMqMockServer client = new RabbitMqMockServer(PORT);
      mockResponses(client);
      InMemoryConfigStore.set(ConfigName.EVENT_DISPATCHER_HOST, HOST);
      InMemoryConfigStore.set(ConfigName.EVENT_DISPATCHER_PORT, Integer.toString(PORT));
      return client;
    }, RabbitMqMockServer.class);
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(RabbitMqMockServer.class);
  }

  @Override
  public Object resolveParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(RabbitMqMockServer.class)) {
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (RabbitMqMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
      );
  }

  private void mockResponses(MockServerClient client) {
  }
}
