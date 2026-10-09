// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.spi.Elements;
import com.google.inject.spi.LinkedKeyBinding;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.api.MeetingsApiService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.storage.impl.StoragesServiceImpl;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.ParticipantServiceImpl;
import com.zextras.carbonio.chats.core.web.api.MeetingsApiServiceImpl;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AreaModulesTest {

  private static Injector injector;

  @BeforeAll
  static void createToolInjector() {
    injector =
        Guice.createInjector(
            Stage.TOOL,
            new ProductionConfig(),
            new CoreModule(),
            new DatabaseModule(),
            new StorageModule(),
            new MeetingModule());
  }

  @Test
  void areaModulesBindCeImplementations() {
    assertLinkedTo(StoragesService.class, StoragesServiceImpl.class);
    assertLinkedTo(MeetingService.class, MeetingServiceImpl.class);
    assertLinkedTo(MeetingsApiService.class, MeetingsApiServiceImpl.class);
    assertLinkedTo(ParticipantService.class, ParticipantServiceImpl.class);
    assertLinkedTo(VideoServerService.class, VideoServerServiceImpl.class);
    assertLinkedTo(MeetingMapper.class, MeetingMapperImpl.class);
  }

  @Test
  void coreModuleBindsNoAreaModuleKey() {
    Set<Key<?>> coreKeys = boundKeys(new CoreModule());
    for (Key<?> areaKey :
        boundKeys(new DatabaseModule(), new StorageModule(), new MeetingModule())) {
      assertFalse(coreKeys.contains(areaKey), areaKey + " must only be bound by its area module");
    }
  }

  @Test
  void dataSourceIsSharedInCoreModule() {
    assertTrue(boundKeys(new CoreModule()).contains(Key.get(HikariDataSource.class)));
  }

  private static void assertLinkedTo(Class<?> type, Class<?> impl) {
    assertEquals(
        Key.get(impl),
        assertInstanceOf(LinkedKeyBinding.class, injector.getBinding(type)).getLinkedKey());
  }

  private static Set<Key<?>> boundKeys(Module... modules) {
    return Elements.getElements(Stage.TOOL, modules).stream()
        .filter(Binding.class::isInstance)
        .map(element -> ((Binding<?>) element).getKey())
        .collect(Collectors.toSet());
  }
}
