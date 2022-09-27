package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import com.zextras.carbonio.usermanagement.exceptions.UserNotFound;
import io.vavr.control.Try;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementProfilingServiceTest {

  private final UserManagementProfilingService profilingService;
  private final UserManagementClient           userManagementClient;

  public UserManagementProfilingServiceTest() {
    this.userManagementClient = mock(UserManagementClient.class);
    this.profilingService = new UserManagementProfilingService(userManagementClient);
  }

  @AfterEach
  public void cleanup() {
    reset(userManagementClient);
  }

  @Nested
  @DisplayName("Get by id tests")
  class GetByIdTests {

    @Test
    @DisplayName("Returns the requested user correctly mapped")
    public void getById_testOk() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementClient.getUserByUUID("ZM_AUTH_TOKEN=cookie", randomUUID))
        .thenReturn(
          Try.success(new UserInfo(randomUUID.toString(), "email@test.com", "name hello", "mydomain.com"))
        );
      Map<AuthenticationMethod, String> credentials = Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "cookie");
      Optional<UserProfile> userProfile = profilingService.getById(
        UserPrincipal.create(randomUUID).authCredentials(credentials), randomUUID
      );

      assertTrue(userProfile.isPresent());
      assertEquals(randomUUID.toString(), userProfile.get().getId());
      assertEquals("email@test.com", userProfile.get().getEmail());
      assertEquals("name hello", userProfile.get().getName());
      assertEquals("mydomain.com", userProfile.get().getDomain());
    }

    @Test
    @DisplayName("Returns an empty optional if the user was not found")
    public void getById_testNotFound() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementClient.getUserByUUID("ZM_AUTH_TOKEN=cookie", randomUUID))
        .thenReturn(
          Try.failure(new UserNotFound(randomUUID.toString()))
        );
      Map<AuthenticationMethod, String> credentials = Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "cookie");
      Optional<UserProfile> userProfile = profilingService.getById(
        UserPrincipal.create(randomUUID).authCredentials(credentials), randomUUID
      );

      assertTrue(userProfile.isEmpty());
    }

    @Test
    @DisplayName("Throws a forbidden exception when the user doesn't have an authentication token")
    public void getById_testForbiddenException() {
      UUID randomUUID = UUID.randomUUID();
      Map<AuthenticationMethod, String> credentials = Map.of();
      assertThrows(ForbiddenException.class, () -> profilingService.getById(
        UserPrincipal.create(randomUUID).authCredentials(credentials), randomUUID
      ));
    }

    @Test
    @DisplayName("Throws an exception when the call fails for any other reason")
    public void getById_testException() {
      UUID randomUUID = UUID.randomUUID();
      when(userManagementClient.getUserByUUID("ZM_AUTH_TOKEN=cookie", randomUUID))
        .thenReturn(
          Try.failure(new Exception())
        );
      Map<AuthenticationMethod, String> credentials = Map.of(AuthenticationMethod.ZM_AUTH_TOKEN, "cookie");
      assertThrows(InternalErrorException.class, () -> profilingService.getById(
        UserPrincipal.create(randomUUID).authCredentials(credentials), randomUUID
      ));
    }

  }

}