package com.zextras.carbonio.chats.core.infrastructure.authentication.impl;

import com.zextras.carbonio.chats.core.infrastructure.authentication.AuthenticationService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import io.vavr.control.Try;
import java.util.Map;
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
  public Optional<String> validateToken(Map<AuthenticationMethod, String> credentials) {
    return Optional.ofNullable(credentials)
      .map(credentialMap -> credentialMap.get(AuthenticationMethod.ZM_AUTH_TOKEN))
      .map(token -> userManagementClient.validateUserToken(token).map(UserId::getUserId).getOrElse(() -> null));
  }

  @Override
  public Optional<UserProfile> getByUUID(UUID userId, UserPrincipal currentUser) {
    if (currentUser.getAuthCredentialFor(AuthenticationMethod.ZM_AUTH_TOKEN).isPresent()) {
      Try<UserInfo> userByUUID = userManagementClient.getUserByUUID(
        currentUser.getAuthCredentialFor(AuthenticationMethod.ZM_AUTH_TOKEN).get(), userId);
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
