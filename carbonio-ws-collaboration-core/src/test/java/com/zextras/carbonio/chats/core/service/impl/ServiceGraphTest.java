// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class ServiceGraphTest {

  private static final List<Class<?>> SERVICES =
      List.of(
          RoomAccessImpl.class,
          RoomServiceImpl.class,
          RoomLifecycleServiceImpl.class,
          MembersServiceImpl.class,
          MeetingServiceImpl.class,
          ParticipantServiceImpl.class,
          AttachmentServiceImpl.class,
          ChatMessageServiceImpl.class,
          ChatExportServiceImpl.class,
          UserServiceImpl.class);

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static AbstractModule serviceModule(List<Class<?>> impls) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        binder().disableCircularProxies();
        Set<Class<?>> serviceTypes = new HashSet<>();
        for (Class<?> impl : impls) {
          serviceTypes.add(impl);
          serviceTypes.addAll(Arrays.asList(impl.getInterfaces()));
        }
        for (Class<?> impl : impls) {
          bind((Class) impl.getInterfaces()[0]).to((Class) impl).in(Scopes.SINGLETON);
        }
        Set<Key<?>> mocked = new HashSet<>();
        for (Class<?> impl : impls) {
          for (Constructor<?> constructor : impl.getDeclaredConstructors()) {
            if (!constructor.isAnnotationPresent(Inject.class)
                && !constructor.isAnnotationPresent(jakarta.inject.Inject.class)) {
              continue;
            }
            for (java.lang.reflect.Parameter parameter : constructor.getParameters()) {
              if (serviceTypes.contains(parameter.getType())) {
                continue;
              }
              Key<?> key = Key.get(parameter.getParameterizedType());
              if (mocked.add(key)) {
                bind((Key) key).toInstance(mock(parameter.getType()));
              }
            }
          }
        }
      }
    };
  }

  @Test
  @DisplayName("The real service implementations build without circular proxies")
  void serviceImplementationsHaveNoCycle() {
    Injector injector = assertDoesNotThrow(() -> Guice.createInjector(serviceModule(SERVICES)));

    for (Class<?> impl : SERVICES) {
      Class<?> service = impl.getInterfaces()[0];
      assertNotNull(
          assertDoesNotThrow(
              () -> injector.getInstance(service),
              () -> "Cycle or unsatisfied dependency building " + service.getSimpleName()));
    }
  }

  interface First {}

  interface Second {}

  static class FirstImpl implements First {
    @Inject
    FirstImpl(Second second) {}
  }

  static class SecondImpl implements Second {
    @Inject
    SecondImpl(First first) {}
  }

  @Test
  @DisplayName("The same wiring fails when two services depend on each other")
  void aReintroducedCycleFails() {
    Injector injector =
        Guice.createInjector(serviceModule(List.of(FirstImpl.class, SecondImpl.class)));

    assertThrows(ProvisionException.class, () -> injector.getInstance(First.class));
  }
}
