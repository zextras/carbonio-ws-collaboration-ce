// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.data.type.UserType;
import jakarta.annotation.Nullable;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UserPrincipal implements Principal {

  private UserType userType;
  @Nullable private UUID userId;
  private UUID queueId;
  private String email;
  private String name;
  @Nullable private String authToken;

  public UserPrincipal() {}

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
    return name;
  }

  public UserPrincipal name(String name) {
    this.name = name;
    return this;
  }

  public String getId() {
    return userId != null ? userId.toString() : null;
  }

  public UserType getUserType() {
    return userType;
  }

  public UserPrincipal userType(UserType userType) {
    this.userType = userType;
    return this;
  }

  public UUID getUUID() {
    return userId;
  }

  public UserPrincipal id(UUID userId) {
    this.userId = userId;
    return this;
  }

  public UUID getQueueId() {
    return queueId;
  }

  public UserPrincipal queueId(UUID queueId) {
    this.queueId = queueId;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public UserPrincipal email(String email) {
    this.email = email;
    return this;
  }

  public Optional<String> getAuthToken() {
    return Optional.ofNullable(authToken);
  }

  public UserPrincipal authToken(@Nullable String authToken) {
    this.authToken = authToken;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserPrincipal that)) return false;
    return getUserType() == that.getUserType()
        && Objects.equals(userId, that.userId)
        && Objects.equals(getQueueId(), that.getQueueId())
        && Objects.equals(getEmail(), that.getEmail());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUserType(), userId, getQueueId(), getEmail());
  }

  @Override
  public String toString() {
    return userId != null ? userId.toString() : null;
  }
}
