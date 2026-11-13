// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import jakarta.annotation.Nullable;

public class MessageDispatcherCredentials {

  private final String host;
  private final int port;
  private final Supplier<Secrets> secrets;

  private record Secrets(String name, String username, String password) {}

  public MessageDispatcherCredentials(
      String host,
      int port,
      @Nullable String name,
      @Nullable String username,
      @Nullable String password) {
    this(host, port, () -> new Secrets(name, username, password));
  }

  private MessageDispatcherCredentials(String host, int port, Supplier<Secrets> secrets) {
    this.host = host;
    this.port = port;
    this.secrets = Suppliers.memoize(secrets);
  }

  public static MessageDispatcherCredentials lazy(
      String host, int port, MessageDispatcherCredentialReader reader) {
    return new MessageDispatcherCredentials(
        host,
        port,
        () ->
            new Secrets(
                reader.read(ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME).orElse(null),
                reader.read(ConfigName.MESSAGE_DISPATCHER_DATABASE_USERNAME).orElse(null),
                reader.read(ConfigName.MESSAGE_DISPATCHER_DATABASE_PASSWORD).orElse(null)));
  }

  public boolean isAvailable() {
    Secrets s = secrets.get();
    return s.name() != null && s.username() != null && s.password() != null;
  }

  public String getJdbcUrl() {
    return "jdbc:postgresql://%s:%d/%s".formatted(host, port, getName());
  }

  public String getName() {
    return secrets.get().name();
  }

  public String getUsername() {
    return secrets.get().username();
  }

  public String getPassword() {
    return secrets.get().password();
  }
}
