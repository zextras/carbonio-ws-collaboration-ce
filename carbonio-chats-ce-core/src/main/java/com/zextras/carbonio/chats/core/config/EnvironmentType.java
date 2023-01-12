// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

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
