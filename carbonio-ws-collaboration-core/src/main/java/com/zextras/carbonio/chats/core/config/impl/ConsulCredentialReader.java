// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentialReader;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reads a Consul KV key without swallowing errors: an absent key (404) is empty, every other
 * failure, including a missing token, is thrown.
 */
public class ConsulCredentialReader implements MessageDispatcherCredentialReader {

  private final Supplier<Consul> consul;
  private final String token;
  private final Map<String, String> consulKeys;

  public ConsulCredentialReader(
      Supplier<Consul> consul, @Nullable String token, Map<String, String> consulKeys) {
    this.consul = consul;
    this.token = token;
    this.consulKeys = consulKeys;
  }

  @Override
  public Optional<String> read(ConfigName name) {
    String consulKey = consulKeys.get(name.name());
    if (consulKey == null) {
      throw new IllegalStateException("No Consul key mapped for " + name);
    }
    if (token == null || token.isBlank()) {
      throw new IllegalStateException(
          "CONSUL_HTTP_TOKEN is not set: cannot read " + consulKey + " from Consul");
    }
    try {
      return consul
          .get()
          .keyValueClient()
          .getValue(consulKey, ImmutableQueryOptions.builder().token(token).build())
          .flatMap(Value::getValueAsString);
    } catch (RuntimeException e) {
      throw new IllegalStateException("Unable to read " + consulKey + " from Consul", e);
    }
  }
}
