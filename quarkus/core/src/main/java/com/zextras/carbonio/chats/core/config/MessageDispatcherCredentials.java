// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import jakarta.annotation.Nullable;

public class MessageDispatcherCredentials {

  private final String host;
  private final int port;
  private final String name;
  private final String username;
  private final String password;

  public MessageDispatcherCredentials(
      String host,
      int port,
      @Nullable String name,
      @Nullable String username,
      @Nullable String password) {
    this.host = host;
    this.port = port;
    this.name = name;
    this.username = username;
    this.password = password;
  }

  public boolean isAvailable() {
    return name != null && username != null && password != null;
  }

  public String getJdbcUrl() {
    return "jdbc:postgresql://%s:%d/%s".formatted(host, port, name);
  }

  public String getName() {
    return name;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
