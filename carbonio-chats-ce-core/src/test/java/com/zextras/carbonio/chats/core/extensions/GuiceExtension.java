package com.zextras.carbonio.chats.core.extensions;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.RoomMapperImpl;
import com.zextras.carbonio.chats.core.mapper.RoomUserSettingsMapper;
import com.zextras.carbonio.chats.core.mapper.RoomUserSettingsMapperImpl;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapperImpl;
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
  public void beforeAll(ExtensionContext context) {
    context.getStore(EXTENSION_NAMESPACE)
      .put(GUICE_STORE_ENTRY,
        Guice.createInjector(new AbstractModule() {
          @Override
          protected void configure() {
            bind(RoomMapper.class).to(RoomMapperImpl.class);
            bind(SubscriptionMapper.class).to(SubscriptionMapperImpl.class);
            bind(RoomUserSettingsMapper.class).to(RoomUserSettingsMapperImpl.class);
            bind(AttachmentMapper.class).to(AttachmentMapperImpl.class);
          }
        })
      );
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
