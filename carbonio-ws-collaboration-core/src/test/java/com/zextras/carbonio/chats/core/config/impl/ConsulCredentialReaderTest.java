// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.ImmutableValue;
import com.orbitz.consul.option.QueryOptions;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class ConsulCredentialReaderTest {

  private static final Map<String, String> KEYS =
      Map.of(
          ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME.name(),
          "carbonio-message-dispatcher-db/db-name",
          ConfigName.MESSAGE_DISPATCHER_DATABASE_USERNAME.name(),
          "carbonio-message-dispatcher-db/db-username",
          ConfigName.MESSAGE_DISPATCHER_DATABASE_PASSWORD.name(),
          "carbonio-message-dispatcher-db/db-password");

  private final Consul consul = mock(Consul.class);
  private final KeyValueClient keyValueClient = mock(KeyValueClient.class);

  ConsulCredentialReaderTest() {
    when(consul.keyValueClient()).thenReturn(keyValueClient);
  }

  private ConsulCredentialReader reader(String token) {
    return new ConsulCredentialReader(() -> consul, token, KEYS);
  }

  private static ImmutableValue value(String key, String content) {
    return ImmutableValue.builder()
        .createIndex(1)
        .modifyIndex(1)
        .lockIndex(0)
        .key(key)
        .flags(0)
        .value(Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)))
        .build();
  }

  @Test
  @DisplayName("A key present in Consul is returned")
  void read_keyPresent_returnsValue() {
    when(keyValueClient.getValue(
            eq("carbonio-message-dispatcher-db/db-name"), any(QueryOptions.class)))
        .thenReturn(Optional.of(value("carbonio-message-dispatcher-db/db-name", "mongooseim")));

    assertEquals(
        Optional.of("mongooseim"),
        reader("TOKEN").read(ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME));
  }

  @Test
  @DisplayName("A key absent from Consul (404) is empty and all-absent credentials are unavailable")
  void read_keyAbsent_returnsEmpty() {
    when(keyValueClient.getValue(any(String.class), any(QueryOptions.class)))
        .thenReturn(Optional.empty());

    ConsulCredentialReader reader = reader("TOKEN");

    assertEquals(Optional.empty(), reader.read(ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME));
    assertFalse(MessageDispatcherCredentials.lazy("localhost", 5432, reader).isAvailable());
  }

  @Test
  @DisplayName("Any Consul failure other than an absent key is thrown")
  void read_consulFailure_throws() {
    when(keyValueClient.getValue(any(String.class), any(QueryOptions.class)))
        .thenThrow(new ConsulException("Consul request failed with status 403"));

    assertThrows(
        IllegalStateException.class,
        () -> reader("TOKEN").read(ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME));
  }

  @Test
  @DisplayName("A missing token fails without querying Consul")
  void read_noToken_throws() {
    assertThrows(
        IllegalStateException.class,
        () -> reader(null).read(ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME));
    assertThrows(
        IllegalStateException.class,
        () -> reader(" ").read(ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME));
    verifyNoInteractions(keyValueClient);
  }

  @Test
  @DisplayName("Lazy credentials resolve all three keys when first used and then stay cached")
  void lazyCredentials_resolveOnFirstUse() {
    when(keyValueClient.getValue(any(String.class), any(QueryOptions.class)))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              return Optional.of(value(key, key.substring(key.indexOf('/') + 1)));
            });

    MessageDispatcherCredentials credentials =
        MessageDispatcherCredentials.lazy("localhost", 5432, reader("TOKEN"));

    verifyNoInteractions(keyValueClient);
    assertTrue(credentials.isAvailable());
    assertEquals("jdbc:postgresql://localhost:5432/db-name", credentials.getJdbcUrl());
    assertEquals("db-username", credentials.getUsername());
    assertEquals("db-password", credentials.getPassword());
  }

  @Test
  @DisplayName("Lazy credentials fail when Consul fails, so the migration fails")
  void lazyCredentials_consulFailure_throws() {
    when(keyValueClient.getValue(any(String.class), any(QueryOptions.class)))
        .thenThrow(new ConsulException("Consul request failed with status 500"));

    MessageDispatcherCredentials credentials =
        MessageDispatcherCredentials.lazy("localhost", 5432, reader("TOKEN"));

    assertThrows(IllegalStateException.class, credentials::isAvailable);
  }
}
