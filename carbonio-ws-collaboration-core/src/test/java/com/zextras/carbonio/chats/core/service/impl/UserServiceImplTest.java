// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserServiceImplTest {

  private final UserService userService;
  private final ProfilingService profilingService;
  private final UserRepository userRepository;

  public UserServiceImplTest() {
    this.profilingService = mock(ProfilingService.class);
    this.userRepository = mock(UserRepository.class);
    this.userService = new UserServiceImpl(profilingService, userRepository);
  }

  @Nested
  @DisplayName("Get user by id tests")
  class GetUserByIdTests {

    @Test
    @DisplayName("Returns the user with every info")
    void getUserById_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString()))
          .thenReturn(
              Optional.of(
                  User.create().id(requestedUserId.toString()).statusMessage("my status!")));
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(
              Optional.of(
                  UserProfile.create(requestedUserId)
                      .email("test@example.com")
                      .domain("mydomain.com")
                      .name("test user")));

      UserDto userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertEquals("my status!", userById.getStatusMessage());
      assertEquals("test user", userById.getName());
      assertEquals("test@example.com", userById.getEmail());
    }

    @Test
    @DisplayName("Returns the user with the profiling info if it's not found in out db")
    void getUserById_testDbNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(
              Optional.of(
                  UserProfile.create(requestedUserId)
                      .email("test@example.com")
                      .domain("mydomain.com")
                      .name("test user")));

      UserDto userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertNull(userById.getStatusMessage());
      assertEquals("test user", userById.getName());
      assertEquals("test@example.com", userById.getEmail());
    }

    @Test
    @DisplayName("Throws NotFoundException if the user was not found using the profiling service")
    void getUserById_testProfilingNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> userService.getUserById(requestedUserId, currentPrincipal));
    }
  }

  @Nested
  @DisplayName("Get users by ids tests")
  class GetUsersByIdsTests {

    @Test
    @DisplayName("Returns all the users with every info")
    void getUsersByIds_testOk() {
      UUID requestedUserId1 = UUID.randomUUID();
      UUID requestedUserId2 = UUID.randomUUID();
      UUID requestedUserId3 = UUID.randomUUID();
      List<UUID> requestedUserIds =
          Arrays.asList(requestedUserId1, requestedUserId2, requestedUserId3);
      List<String> strUserIds = requestedUserIds.stream().map(UUID::toString).toList();

      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId1);

      when(userRepository.getByIds(strUserIds))
          .thenReturn(
              Arrays.asList(
                  User.create().id(requestedUserId1.toString()).statusMessage("my status 1"),
                  User.create().id(requestedUserId2.toString()).statusMessage("my status 2"),
                  User.create().id(requestedUserId3.toString()).statusMessage("my status 3")));
      when(profilingService.getByIds(currentPrincipal, strUserIds))
          .thenReturn(
              Arrays.asList(
                  UserProfile.create(requestedUserId1)
                      .email("test1@example.com")
                      .domain("mydomain.com")
                      .name("test user 1"),
                  UserProfile.create(requestedUserId2)
                      .email("test2@example.com")
                      .domain("mydomain.com")
                      .name("test user 2"),
                  UserProfile.create(requestedUserId3)
                      .email("test3@example.com")
                      .domain("mydomain.com")
                      .name("test user 3")));

      List<UserDto> usersById = userService.getUsersByIds(requestedUserIds, currentPrincipal);

      assertFalse(usersById.isEmpty());
      assertEquals(requestedUserId1, usersById.get(0).getId());
      assertEquals("my status 1", usersById.get(0).getStatusMessage());
      assertEquals("test user 1", usersById.get(0).getName());
      assertEquals("test1@example.com", usersById.get(0).getEmail());
      assertEquals(requestedUserId2, usersById.get(1).getId());
      assertEquals("my status 2", usersById.get(1).getStatusMessage());
      assertEquals("test user 2", usersById.get(1).getName());
      assertEquals("test2@example.com", usersById.get(1).getEmail());
      assertEquals(requestedUserId3, usersById.get(2).getId());
      assertEquals("my status 3", usersById.get(2).getStatusMessage());
      assertEquals("test user 3", usersById.get(2).getName());
      assertEquals("test3@example.com", usersById.get(2).getEmail());
    }

    @Test
    @DisplayName("Don't returns duplicates")
    void getUsersByIds_testNoDuplicates() {
      UUID requestedUserId1 = UUID.randomUUID();
      List<UUID> requestedUserIds = Arrays.asList(requestedUserId1, requestedUserId1);
      List<String> strUserIds = requestedUserIds.stream().map(UUID::toString).toList();

      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId1);

      when(userRepository.getByIds(strUserIds))
          .thenReturn(
              Collections.singletonList(
                  User.create().id(requestedUserId1.toString()).statusMessage("my status 1")));
      when(profilingService.getByIds(currentPrincipal, strUserIds))
          .thenReturn(
              Collections.singletonList(
                  UserProfile.create(requestedUserId1)
                      .email("test1@example.com")
                      .domain("mydomain.com")
                      .name("test user 1")));

      List<UserDto> usersById = userService.getUsersByIds(requestedUserIds, currentPrincipal);

      assertFalse(usersById.isEmpty());
      assertEquals(1, usersById.size());
    }

    @Test
    @DisplayName("Returns empty list")
    void getUsersByIds_testEmptyList() {
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(userRepository.getByIds(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(profilingService.getByIds(currentPrincipal, Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      assertTrue(userService.getUsersByIds(Collections.emptyList(), currentPrincipal).isEmpty());
    }

    @Test
    @DisplayName("Returns the user with the profiling info if it's not found in out db")
    void getUsersByIds_testDbNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getByIds(Collections.singletonList(requestedUserId.toString())))
          .thenReturn(Collections.emptyList());
      when(profilingService.getByIds(
              currentPrincipal, Collections.singletonList(requestedUserId.toString())))
          .thenReturn(
              Collections.singletonList(
                  UserProfile.create(requestedUserId)
                      .email("test@example.com")
                      .domain("mydomain.com")
                      .name("test user")));

      List<UserDto> usersById =
          userService.getUsersByIds(Collections.singletonList(requestedUserId), currentPrincipal);

      assertFalse(usersById.isEmpty());
      assertEquals(requestedUserId, usersById.get(0).getId());
      assertNull(usersById.get(0).getStatusMessage());
      assertEquals("test user", usersById.get(0).getName());
      assertEquals("test@example.com", usersById.get(0).getEmail());
    }

    @Test
    @DisplayName("Return an empty list if the user was not found using the profiling service")
    void getUserById_testProfilingNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getByIds(Collections.singletonList(requestedUserId.toString())))
          .thenReturn(Collections.emptyList());
      when(profilingService.getByIds(
              currentPrincipal, Collections.singletonList(requestedUserId.toString())))
          .thenReturn(Collections.emptyList());

      List<UserDto> usersById =
          userService.getUsersByIds(Collections.singletonList(requestedUserId), currentPrincipal);

      assertTrue(usersById.isEmpty());
    }
  }

  @Nested
  @DisplayName("User exists tests")
  class UserExistsTests {

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    void userExists_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(Optional.of(UserProfile.create(requestedUserId)));
      assertTrue(userService.userExists(requestedUserId, currentPrincipal));
    }

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    void userExists_testUserDoesNotExist() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(Optional.empty());

      assertFalse(userService.userExists(requestedUserId, currentPrincipal));
    }
  }
}
