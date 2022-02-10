package com.zextras.carbonio.chats.core.config;

public enum EnvironmentType {
  DEVELOPMENT ("dev"),
  TEST("test"),
  PRODUCTION("prod");

  private final String name;

  EnvironmentType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static EnvironmentType getByName(String name) {
    for (EnvironmentType e : values()) {
      if (e.getName().equals(name)) {
        return e;
      }
    }
    return null;
  }


}
