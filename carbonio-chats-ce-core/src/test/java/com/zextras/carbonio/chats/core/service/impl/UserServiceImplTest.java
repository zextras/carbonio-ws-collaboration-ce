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
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserServiceImplTest {

  private final ProfilingService profilingService;
  private final UserRepository   userRepository;
  private final UserServiceImpl  userService;

  public UserServiceImplTest() {
    this.profilingService = mock(ProfilingService.class);
    this.userRepository = mock(UserRepository.class);
    this.userService = new UserServiceImpl(
      profilingService,
      userRepository
    );
  }

  @Nested
  @DisplayName("Get user by id tests")
  class GetUserByIdTests {

    @Test
    @DisplayName("Returns the user with every info")
    public void getUserById_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(
        Optional.of(
          User.create().id(requestedUserId.toString())
            .lastSeen(OffsetDateTime.of(2022, 12, 1, 12, 12, 12, 12, ZoneOffset.UTC))
            .hash("12345").statusMessage("my status!"))
      );
      when(profilingService.getById(currentPrincipal, requestedUserId))
        .thenReturn(Optional.of(
          UserProfile.create(requestedUserId).email("test@example.com").domain("mydomain.com").name("test user")));

      UserDto userById = userService.getUserByIdRefactor(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertEquals(OffsetDateTime.of(2022, 12, 1, 12, 12, 12, 12, ZoneOffset.UTC).toEpochSecond(),
        userById.getLastSeen());
      assertEquals("my status!", userById.getStatusMessage());
      assertEquals("test user", userById.getName());
      assertEquals("test@example.com", userById.getEmail());
    }

    @Test
    @DisplayName("Returns the user with the profiling info if it's not found in out db")
    public void getUserById_testDbNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId))
        .thenReturn(Optional.of(
          UserProfile.create(requestedUserId).email("test@example.com").domain("mydomain.com").name("test user"))
        );

      UserDto userById = userService.getUserByIdRefactor(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertNull(userById.getLastSeen());
      assertNull(userById.getStatusMessage());
      assertEquals("test user", userById.getName());
      assertEquals("test@example.com", userById.getEmail());
    }

    @Test
    @DisplayName("Throws NotFoundException if the user was not found using the profiling service")
    public void getUserById_testProfilingNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(Optional.empty());

      assertThrows(NotFoundException.class, () -> userService.getUserByIdRefactor(requestedUserId, currentPrincipal));
    }

  }

  @Nested
  @DisplayName("User exists tests")
  class UserExistsTests {

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    public void userExists_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(
        Optional.of(UserProfile.create(requestedUserId)));

      assertTrue(userService.userExists(requestedUserId, currentPrincipal));
    }

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    public void userExists_testUserDoesNotExist() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(Optional.empty());

      assertFalse(userService.userExists(requestedUserId, currentPrincipal));
    }

  }

}