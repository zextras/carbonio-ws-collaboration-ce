package com.zextras.carbonio.chats.it.extensions;

import com.google.inject.Injector;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.exception.handler.ChatsHttpExceptionHandler;
import com.zextras.carbonio.chats.core.exception.handler.DefaultExceptionHandler;
import com.zextras.carbonio.chats.core.exception.handler.XmppServerExceptionHandler;
import com.zextras.carbonio.chats.core.web.security.AuthenticationFilter;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import java.util.Optional;
import org.jboss.resteasy.mock.MockDispatcherFactory;
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
    dispatcher.getProviderFactory().registerProvider(JacksonConfig.class);
    dispatcher.getProviderFactory().registerProviderInstance(
      injector.getInstance(AuthenticationFilter.class)
    );

    dispatcher.getProviderFactory().registerProvider(ChatsHttpExceptionHandler.class);
    dispatcher.getProviderFactory().registerProvider(DefaultExceptionHandler.class);
    dispatcher.getProviderFactory().registerProvider(XmppServerExceptionHandler.class);

    return dispatcher;
  }
}
