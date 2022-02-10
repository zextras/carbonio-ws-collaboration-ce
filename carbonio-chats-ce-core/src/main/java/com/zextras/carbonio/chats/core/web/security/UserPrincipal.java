package com.zextras.carbonio.chats.core.web.security;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;


public class UserPrincipal implements Principal {

  private String  userId;
  private boolean systemUser = false;
  private String  cookie;


  public UserPrincipal() {

  }

  public UserPrincipal(String userId) {
    this.userId = userId;
  }

  public static UserPrincipal create(String userId) {
    return new UserPrincipal(userId);
  }

  public static UserPrincipal create(UUID userId) {
    return new UserPrincipal(userId.toString());
  }

  @Override
  public String getName() {
    return userId;
  }

  public String getId() {
    return userId;
  }

  public UUID getUUID() {
    return UUID.fromString(userId);
  }

  public UserPrincipal id(String userId) {
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

  public String getCookie() {
    return cookie;
  }

  public UserPrincipal cookie(String cookie) {
    this.cookie = cookie;
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
    return userId;
  }
}
