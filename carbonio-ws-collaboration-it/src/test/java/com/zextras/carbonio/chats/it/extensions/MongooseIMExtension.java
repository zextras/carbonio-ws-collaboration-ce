// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.MongooseImMockServer;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.model.ClearType;
import org.testcontainers.shaded.com.trilead.ssh2.packets.PacketSessionStartShell;

public class MongooseIMExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private static final Namespace EXTENSION_NAMESPACE = Namespace.create(MongooseIMExtension.class);
  private static final String    CLIENT_STORE_ENTRY  = "mongoose_client";
  private static final int       PORT                = 12345;
  private static final String    HOST                = "localhost";
  private static final String    USERNAME            = "username";
  private static final String    PASSWORD            = "password";

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(CLIENT_STORE_ENTRY, (key) -> {
      ChatsLogger.debug("Starting MongooseIM client mock...");
      MongooseImMockServer client = new MongooseImMockServer(PORT);
      InMemoryConfigStore.set(ConfigName.XMPP_SERVER_HOST, HOST);
      InMemoryConfigStore.set(ConfigName.XMPP_SERVER_HTTP_PORT, Integer.toString(PORT));
      InMemoryConfigStore.set(ConfigName.XMPP_SERVER_USERNAME, USERNAME);
      InMemoryConfigStore.set(ConfigName.XMPP_SERVER_PASSWORD, PASSWORD);
      return client;
    }, MongooseImMockServer.class);
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
      return Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .orElseThrow(() -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
      .map(mock -> (MongooseImMockServer) mock)
      .ifPresent(
        mock -> mock.clear(request(), ClearType.LOG)
      );
  }
}
