package com.zextras.carbonio.chats.core.web.security;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;


public class UserPrincipal implements Principal {

  @Nullable
  private UUID                              userId;
  private boolean                           systemUser = false;
  private Map<AuthenticationMethod, String> authCredentials;

  public UserPrincipal() {

  }

  public UserPrincipal(@Nullable UUID userId) {
    this.userId = userId;
  }

  public static UserPrincipal create(String userId) {
    return new UserPrincipal(UUID.fromString(userId));
  }

  public static UserPrincipal create(UUID userId) {
    return new UserPrincipal(userId);
  }

  @Override
  public String getName() {
    return userId != null ? userId.toString() : null;
  }

  public String getId() {
    return userId != null ? userId.toString() : null;
  }

  public UUID getUUID() {
    return userId;
  }

  public UserPrincipal id(UUID userId) {
    this.userId = userId;
    return this;
  }

  public boolean isSystemUser() {
    return systemUser;
  }

  public UserPrincipal systemUser(boolean systemUser) {
    this.systemUser = systemUser;
    return this;
  }

  public Map<AuthenticationMethod, String> getAuthCredentials() {
    return new HashMap<>(authCredentials);
  }

  public Optional<String> getAuthCredentialFor(AuthenticationMethod method) {
    return Optional.ofNullable(authCredentials).map(credentialsMap -> credentialsMap.get(method));
  }

  public UserPrincipal authCredentials(Map<AuthenticationMethod, String> authCredentials) {
    this.authCredentials = authCredentials;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPrincipal that = (UserPrincipal) o;
    return Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }

  @Override
  public String toString() {
    return userId != null ? userId.toString() : null;
  }
}
