// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.PreviewerMockServer;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockserver.model.ClearType;

public class PreviewerExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 7894;
  private static final Namespace EXTENSION_NAMESPACE = Namespace.create(PreviewerExtension.class);
  private static final String CLIENT_STORE_ENTRY = "preview_client";

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .getOrComputeIfAbsent(
            CLIENT_STORE_ENTRY,
            (key) -> {
              ChatsLogger.debug("Starting Previewer mock...");
              PreviewerMockServer client = new PreviewerMockServer(SERVER_PORT);
              InMemoryConfigStore.set(ConfigName.PREVIEWER_HOST, SERVER_HOST);
              InMemoryConfigStore.set(ConfigName.PREVIEWER_PORT, Integer.toString(SERVER_PORT));
              return client;
            },
            PreviewerMockServer.class);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(PreviewerMockServer.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(PreviewerMockServer.class)) {
      return Optional.ofNullable(
              extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
          .orElseThrow(
              () -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
        .map(mock -> (PreviewerMockServer) mock)
        .ifPresent(mock -> mock.clear(request(), ClearType.LOG));
  }
}
