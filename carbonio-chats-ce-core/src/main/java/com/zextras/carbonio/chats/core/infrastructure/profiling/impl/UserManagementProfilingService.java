package com.zextras.carbonio.chats.core.infrastructure.profiling.impl;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.exceptions.UserNotFound;
import io.vavr.API.Match;
import io.vavr.API.Match.Case;
import java.util.Optional;
import java.util.UUID;

public class UserManagementProfilingService implements ProfilingService {

  private static final String AUTH_COOKIE = "ZM_AUTH_TOKEN";

  private final UserManagementClient userManagementClient;

  public UserManagementProfilingService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<UserProfile> getById(UserPrincipal principal, UUID userId) {
    String token = principal.getAuthCredentialFor(AuthenticationMethod.ZM_AUTH_TOKEN)
      .orElseThrow(ForbiddenException::new);
    return Optional.ofNullable(
      userManagementClient.getUserByUUID(String.format("%s=%s", AUTH_COOKIE, token), userId).map(userInfo ->
        UserProfile.create(userInfo.getId())
          .name(userInfo.getFullName())
          .email(userInfo.getEmail())
          .domain(userInfo.getDomain())
      ).recover(UserNotFound.class, e -> null)
        .getOrElseThrow((fail) -> new InternalErrorException(fail)));
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
