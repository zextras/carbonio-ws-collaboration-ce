// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.extensions;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.impl.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.RoomMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.SubscriptionMapperImpl;
import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class GuiceMappersExtension implements ParameterResolver, BeforeAllCallback {

  private final static Namespace EXTENSION_NAMESPACE = Namespace.create(GuiceMappersExtension.class);
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
            bind(AttachmentMapper.class).to(AttachmentMapperImpl.class);
            bind(MeetingMapper.class).to(MeetingMapperImpl.class);
            bind(ParticipantMapper.class).to(ParticipantMapperImpl.class);
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
