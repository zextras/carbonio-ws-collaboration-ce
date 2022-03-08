package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import java.util.Optional;
import java.util.UUID;

public class UserManagementProfilingService implements ProfilingService {

  private final UserManagementClient userManagementClient;

  public UserManagementProfilingService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<UserProfile> getById(UUID userId) {
    return Optional.empty();
  }

  @Override
  public boolean isAlive() {
    return false;
  }
}
