package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserManagementProfilingServiceTest {

  private UserManagementProfilingService profilingService;
  private UserManagementClient           userManagementClient;

  public UserManagementProfilingServiceTest() {
    this.profilingService = new UserManagementProfilingService(userManagementClient);
  }

  @Nested
  @DisplayName("Get by id tests")
  class GetByIdTests {

    @Test
    @DisplayName("Returns the requested user")
    public void getById_testOk() {

    }

    @Test
    @DisplayName("Returns an empty optional if the user was not found")
    public void getById_testNotFound() {

    }

  }

}