// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.util.Types;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.api.MeetingsApiService;
import com.zextras.carbonio.chats.core.config.module.CoreModule.FlywayProvider;
import com.zextras.carbonio.chats.core.config.module.CoreModule.HikariDataSourceProvider;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.storage.impl.StoragesServiceImpl;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.RoomMapperImpl;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.ParticipantServiceImpl;
import com.zextras.carbonio.chats.core.web.api.MeetingsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CoreModuleTest {

  private static Injector injector;

  @BeforeAll
  static void createToolInjector() {
    injector = Guice.createInjector(Stage.TOOL, new ProductionConfig(), new CoreModule());
  }

  @Test
  void extensionPointsDefaultToCeImplementations() {
    assertDefaultLinkedTo(StoragesService.class, StoragesServiceImpl.class);
    assertDefaultLinkedTo(VideoServerService.class, VideoServerServiceImpl.class);
    assertDefaultLinkedTo(MeetingService.class, MeetingServiceImpl.class);
    assertDefaultLinkedTo(MeetingsApiService.class, MeetingsApiServiceImpl.class);
    assertDefaultLinkedTo(ParticipantService.class, ParticipantServiceImpl.class);
    assertDefaultLinkedTo(RoomMapper.class, RoomMapperImpl.class);
    assertDefaultLinkedTo(MeetingMapper.class, MeetingMapperImpl.class);
  }

  @Test
  void dataSourceAndFlywayDefaultToCeProviders() {
    assertEquals(
        Key.get(HikariDataSourceProvider.class),
        assertInstanceOf(ProviderKeyBinding.class, defaultOf(HikariDataSource.class))
            .getProviderKey());
    assertEquals(
        Key.get(FlywayProvider.class),
        assertInstanceOf(ProviderKeyBinding.class, defaultOf(Flyway.class)).getProviderKey());
  }

  @Test
  void eventsWebSocketManagerDefaultsToItsOwnConstructor() {
    assertEquals(
        EventsWebSocketManager.class,
        assertInstanceOf(ConstructorBinding.class, defaultOf(EventsWebSocketManager.class))
            .getConstructor()
            .getDeclaringType()
            .getRawType());
  }

  private static void assertDefaultLinkedTo(Class<?> type, Class<?> impl) {
    assertEquals(
        Key.get(impl),
        assertInstanceOf(LinkedKeyBinding.class, defaultOf(type)).getLinkedKey(),
        type.getSimpleName());
  }

  private static Binding<?> defaultOf(Class<?> type) {
    OptionalBinderBinding<?> optional = optionalBinderOf(type);
    assertNull(optional.getActualBinding(), type.getSimpleName() + " must not be replaced in CE");
    return optional.getDefaultBinding();
  }

  private static OptionalBinderBinding<?> optionalBinderOf(Class<?> type) {
    Binding<?> binding =
        injector.getBinding(Key.get(Types.newParameterizedType(Optional.class, type)));
    return (OptionalBinderBinding<?>) binding.acceptTargetVisitor(new OptionalBinderVisitor());
  }

  private static class OptionalBinderVisitor extends DefaultBindingTargetVisitor<Object, Object>
      implements MultibindingsTargetVisitor<Object, Object> {

    @Override
    public Object visit(MultibinderBinding<? extends Object> multibinding) {
      return null;
    }

    @Override
    public Object visit(MapBinderBinding<? extends Object> mapbinding) {
      return null;
    }

    @Override
    public Object visit(OptionalBinderBinding<? extends Object> optionalbinding) {
      return optionalbinding;
    }
  }
}
