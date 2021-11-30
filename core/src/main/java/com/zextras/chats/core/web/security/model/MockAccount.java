package com.zextras.chats.core.web.security.model;

import java.util.UUID;

public class MockAccount {

  private UUID   id;
  private String name;


  public MockAccount(String id, String name) {
    this.id = UUID.fromString(id);
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

}
