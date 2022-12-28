// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import com.google.inject.Injector;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import java.util.Optional;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.plugins.guice.ModuleProcessor;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RestEasyExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(ResteasyRequestDispatcher.class);
  }

  @Override
  public Object resolveParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    Injector injector = Optional.ofNullable(extensionContext.getStore(Namespace.GLOBAL).
        get(GuiceExtension.GUICE_STORE_ENTRY))
      .map(objectInjector -> (Injector) objectInjector)
      .orElseThrow(() -> new ParameterResolutionException("No Guice injector found"));
    return new ResteasyRequestDispatcher(getDispatcher(injector));
  }

  private Dispatcher getDispatcher(Injector injector) {
    Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory()).processInjector(injector);
    return dispatcher;
  }
}
