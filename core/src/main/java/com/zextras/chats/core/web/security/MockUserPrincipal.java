package com.zextras.chats.core.web.security;

import java.nio.file.attribute.UserPrincipal;
import java.util.UUID;

public class MockUserPrincipal implements UserPrincipal {

  private final UUID   id;
  private final String name;
  private final boolean isSystemUser;

  public MockUserPrincipal(UUID id, String name, boolean isSystemUser) {
    this.id = id;
    this.name = name;
    this.isSystemUser = isSystemUser;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean isSystemUser() {
    return isSystemUser;
  }
}
