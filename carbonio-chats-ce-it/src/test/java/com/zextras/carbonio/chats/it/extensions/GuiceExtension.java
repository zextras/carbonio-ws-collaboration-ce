package com.zextras.carbonio.chats.it.extensions;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.zextras.carbonio.chats.core.config.CoreModule;
import com.zextras.carbonio.chats.it.config.TestModule;
import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class GuiceExtension implements ParameterResolver, BeforeAllCallback {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(GuiceExtension.class);
  private final static String    GUICE_STORE_ENTRY   = "guice";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    context.getStore(EXTENSION_NAMESPACE)
      .put(GUICE_STORE_ENTRY, Guice.createInjector(Modules.override(new CoreModule()).with(new TestModule())));
  }

  @Override
  public boolean supportsParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return !Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(GUICE_STORE_ENTRY))
      .map(objectInjector -> (Injector) objectInjector)
      .orElseThrow(() -> new ParameterResolutionException("No Guice injector found"))
      .findBindingsByType(TypeLiteral.get(parameterContext.getParameter().getType())).isEmpty();
  }

  @Override
  public Object resolveParameter(
    ParameterContext parameterContext, ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    return Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(GUICE_STORE_ENTRY))
      .map(objectInjector -> (Injector) objectInjector)
      .orElseThrow(() -> new ParameterResolutionException("No Guice injector found"))
      .getInstance(parameterContext.getParameter().getType());
  }
}
