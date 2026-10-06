// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TurnServerServiceTest {

  TurnServerClient client = mock(TurnServerClient.class);
  TurnServerService service = new TurnServerService(client);
  TurnCredentials creds = new TurnCredentials("turn:x", "u", "c", 60);

  @Test
  void preferredInstanceAnswers() {
    when(client.getCredentials("u1", "vs-1")).thenReturn(Optional.of(creds));
    assertThat(service.getCredentials("u1", "vs-1")).contains(creds);
    verify(client, never()).getCredentialsAny("u1");
  }

  @ParameterizedTest
  @ValueSource(ints = {503, 404, 0})
  void unreachablePreferredInstanceFallsBackToAny(int code) {
    when(client.getCredentials("u1", "vs-1")).thenThrow(new TurnServerException("x", code, null));
    when(client.getCredentialsAny("u1")).thenReturn(Optional.of(creds));
    assertThat(service.getCredentials("u1", "vs-1")).contains(creds);
  }

  @Test
  void otherErrorsAreRethrown() {
    when(client.getCredentials("u1", "vs-1")).thenThrow(new TurnServerException("x", 500, null));
    assertThatThrownBy(() -> service.getCredentials("u1", "vs-1")).isInstanceOf(TurnServerException.class);
    verify(client, never()).getCredentialsAny("u1");
  }
}
