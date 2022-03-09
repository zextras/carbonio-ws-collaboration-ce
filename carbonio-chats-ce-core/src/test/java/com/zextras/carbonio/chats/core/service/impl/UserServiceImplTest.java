package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
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

      Optional<UserDto> userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertTrue(userById.isPresent());
      assertEquals(requestedUserId, userById.get().getId());
      assertEquals(OffsetDateTime.of(2022, 12, 1, 12, 12, 12, 12, ZoneOffset.UTC).toEpochSecond(),
        userById.get().getLastSeen());
      assertEquals("my status!", userById.get().getStatusMessage());
      assertEquals("test user", userById.get().getName());
      assertEquals("test@example.com", userById.get().getEmail());
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

      Optional<UserDto> userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertTrue(userById.isPresent());
      assertEquals(requestedUserId, userById.get().getId());
      assertNull(userById.get().getLastSeen());
      assertNull(userById.get().getStatusMessage());
      assertEquals("test user", userById.get().getName());
      assertEquals("test@example.com", userById.get().getEmail());
    }

    @Test
    @DisplayName("Returns and empty optional if the user profiling info were not found")
    public void getUserById_testProfilingNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(Optional.empty());

      Optional<UserDto> userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertTrue(userById.isEmpty());
    }

  }

}