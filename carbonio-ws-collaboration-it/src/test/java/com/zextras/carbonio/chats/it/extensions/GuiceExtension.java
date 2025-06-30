// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.zextras.carbonio.chats.core.config.module.CoreModule;
import com.zextras.carbonio.chats.it.config.TestModule;
import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class GuiceExtension implements ParameterResolver, BeforeAllCallback {

  private static final Namespace EXTENSION_NAMESPACE = Namespace.GLOBAL;
  protected static final String GUICE_STORE_ENTRY = "guice";

  @Override
  public void beforeAll(ExtensionContext context) {
    if (ExtensionUtils.isNestedClass(context)) {
      return;
    }
    context
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .getOrComputeIfAbsent(
            GUICE_STORE_ENTRY,
            (key) ->
                Guice.createInjector(Modules.override(new CoreModule()).with(new TestModule())),
            Injector.class);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return !Optional.ofNullable(
            extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(GUICE_STORE_ENTRY))
        .map(objectInjector -> (Injector) objectInjector)
        .orElseThrow(() -> new ParameterResolutionException("No Guice injector found"))
        .findBindingsByType(TypeLiteral.get(parameterContext.getParameter().getType()))
        .isEmpty();
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return Optional.ofNullable(
            extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(GUICE_STORE_ENTRY))
        .map(objectInjector -> (Injector) objectInjector)
        .orElseThrow(() -> new ParameterResolutionException("No Guice injector found"))
        .getInstance(parameterContext.getParameter().getType());
  }
}
