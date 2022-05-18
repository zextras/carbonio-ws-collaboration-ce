package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.ConsulMockServer;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;

public class ConsulExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private static final String    SERVER_HOST         = "localhost";
  private static final int       SERVER_PORT         = 9876;
  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(ConsulExtension.class);
  private final static String    CLIENT_STORE_ENTRY  = "consul_client";
  private final static String    CONSUL_TOKEN        = "consulToken";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting Consul mock...");
      ConsulMockServer client = new ConsulMockServer(SERVER_PORT);
      mockResponses(client);
      InMemoryConfigStore.set(ConfigName.CONSUL_HOST, SERVER_HOST);
      InMemoryConfigStore.set(ConfigName.CONSUL_PORT, Integer.toString(SERVER_PORT));
      InMemoryConfigStore.set(ConfigName.CONSUL_TOKEN, CONSUL_TOKEN);
      return client;
    }, ConsulMockServer.class);
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(ConsulMockServer.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(ConsulMockServer.class)) {
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (ConsulMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
      );
  }

  private void mockResponses(MockServerClient client) {
    client.when(
      request()
        .withMethod("GET")
        .withPath("/v1/kv/carbonio-chats/")
        .withQueryStringParameter("recurse", null)
        .withQueryStringParameter("token", "consulToken")
    ).respond(
      response()
        .withStatusCode(200)
        .withBody(getConsulValues(
          Map.of(
            "carbonio-chats/hikari-idle-timeout","hikariIdleTimeout",
            "carbonio-chats/hikari-min-pool-size", "hikariMinPoolSize",
            "carbonio-chats/hikari-max-pool-size","hikariMaxPoolSize")))
    );
  }

  private String getConsulValues(Map<String, String> keyValueMap) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    sb.append(keyValueMap.entrySet().stream()
      .map(keyValue -> getConsulValue(keyValue.getKey(), keyValue.getValue()))
      .collect(Collectors.joining(",")));
    sb.append("]");
    return sb.toString();
  }

  private String getConsulValue(String key, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"CreateIndex\": 0,");
    sb.append("\"ModifyIndex\": 0,");
    sb.append("\"LockIndex\": 0,");
    sb.append("\"Flags\": 0,");
    sb.append("\"Session\": \"abc\",");
    sb.append(String.format("\"Key\": \"%s\",", key));
    sb.append(String.format("\"Value\": \"%s\"", Base64.getEncoder().encodeToString(value.getBytes())));
    sb.append("}");
    return sb.toString();
  }
}
