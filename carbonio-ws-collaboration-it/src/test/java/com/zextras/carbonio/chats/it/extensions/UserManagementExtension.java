// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class UserManagementExtension implements BeforeAllCallback, ParameterResolver {

  private static final String SERVER_HOST = "127.0.0.1";
  private static final int SERVER_PORT = 7899;
  private static final Namespace EXTENSION_NAMESPACE =
      Namespace.create(UserManagementExtension.class);
  private static final String CLIENT_STORE_ENTRY = "user_management_client";

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .getOrComputeIfAbsent(
            CLIENT_STORE_ENTRY,
            (key) -> {
              ChatsLogger.debug("Starting User Management mock...");
              UserManagementMockServer client = new UserManagementMockServer(SERVER_PORT);
              InMemoryConfigStore.set(ConfigName.USER_MANAGEMENT_HOST, SERVER_HOST);
              InMemoryConfigStore.set(
                  ConfigName.USER_MANAGEMENT_PORT, Integer.toString(SERVER_PORT));
              return client;
            },
            UserManagementMockServer.class);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(UserManagementMockServer.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(UserManagementMockServer.class)) {
      return Optional.ofNullable(
              extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(CLIENT_STORE_ENTRY))
          .orElseThrow(
              () -> new ParameterResolutionException(parameterContext.getParameter().getName()));
    } else {
      throw new ParameterResolutionException(parameterContext.getParameter().getName());
    }
  }
}
