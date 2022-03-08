package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import io.vavr.control.Try;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserManagementAuthenticationService implements AuthenticationService {

  private final UserManagementClient userManagementClient;

  @Inject
  public UserManagementAuthenticationService(UserManagementClient userManagementClient) {
    this.userManagementClient = userManagementClient;
  }

  @Override
  public Optional<String> validateToken(String token) {
    if (token != null) {
      Try<UserId> userId = userManagementClient.validateUserToken(token);
      if (userId.isSuccess()) {
        return Optional.of(userId.get().getUserId());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<UserProfile> getByUUID(UUID userId, UserPrincipal currentUser) {
    if (currentUser.getCookieString() != null) {
      Try<UserInfo> userByUUID = userManagementClient.getUserByUUID(currentUser.getCookieString(), userId);
      if (userByUUID.isSuccess()) {
        UserInfo userInfo = userByUUID.get();
        return Optional.of(
          UserProfile.create(userInfo.getId())
            .name(userInfo.getFullName())
            .email(userInfo.getEmail())
            .domain(userInfo.getDomain()));
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean isAlive() {
    return userManagementClient.healthCheck();
  }
}
