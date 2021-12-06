package com.zextras.chats.core.web.security.model;

import java.util.UUID;

public class Account {

  private UUID   id;
  private String name;


  public Account(String id, String name) {
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
