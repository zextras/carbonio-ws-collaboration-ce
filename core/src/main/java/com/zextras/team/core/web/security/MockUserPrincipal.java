package com.zextras.team.core.web.security;

import java.nio.file.attribute.UserPrincipal;

public class MockUserPrincipal implements UserPrincipal {

  private final String id;
  private final String name;
  private final String domain;

  public MockUserPrincipal(String id, String name, String domain) {
    this.id = id;
    this.name = name;
    this.domain = domain;
  }

  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getDomain() {
    return domain;
  }
}
