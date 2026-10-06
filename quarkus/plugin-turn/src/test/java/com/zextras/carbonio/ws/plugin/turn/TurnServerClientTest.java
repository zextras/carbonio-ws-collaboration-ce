// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.turn_server.sdk.rest.ApiException;
import com.zextras.carbonio.turn_server.sdk.rest.api.TurnCredentialResourceApi;
import com.zextras.carbonio.turn_server.sdk.rest.model.TurnCredentialsDto;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurnServerClientTest {

  TurnCredentialResourceApi api = mock(TurnCredentialResourceApi.class);
  TurnServerClient client = new TurnServerClient(api);

  @Test
  void preferredCallSendsTheServiceIdHeader() throws Exception {
    when(api.internalTurnCredentialsGet("u1", Map.of("service-id", "vs-1")))
        .thenReturn(new TurnCredentialsDto().configured(true).url("turn:x").username("u").credential("c").ttl(60L));
    assertThat(client.getCredentials("u1", "vs-1")).contains(new TurnCredentials("turn:x", "u", "c", 60));
  }

  @Test
  void notConfiguredMeansEmpty() throws Exception {
    when(api.internalTurnCredentialsGet("u1")).thenReturn(new TurnCredentialsDto().configured(false));
    assertThat(client.getCredentialsAny("u1")).isEmpty();
  }

  @Test
  void missingTtlBecomesZero() throws Exception {
    when(api.internalTurnCredentialsGet("u1"))
        .thenReturn(new TurnCredentialsDto().configured(true).url("turn:x").username("u").credential("c"));
    assertThat(client.getCredentialsAny("u1")).get().extracting(TurnCredentials::ttl).isEqualTo(0L);
  }

  @Test
  void apiErrorsCarryTheHttpCode() throws Exception {
    when(api.internalTurnCredentialsGet("u1", Map.of("service-id", "vs-1"))).thenThrow(new ApiException(503, "down"));
    assertThatThrownBy(() -> client.getCredentials("u1", "vs-1"))
        .isInstanceOfSatisfying(TurnServerException.class, e -> assertThat(e.getCode()).isEqualTo(503));
  }
}
