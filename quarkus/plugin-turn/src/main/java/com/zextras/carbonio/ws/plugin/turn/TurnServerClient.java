// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import com.zextras.carbonio.turn_server.sdk.rest.ApiException;
import com.zextras.carbonio.turn_server.sdk.rest.api.TurnCredentialResourceApi;
import com.zextras.carbonio.turn_server.sdk.rest.model.TurnCredentialsDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TurnServerClient {

  private static final String SERVICE_ID_HEADER = "service-id";

  private final TurnCredentialResourceApi api;

  @Inject
  public TurnServerClient(TurnCredentialResourceApi api) {
    this.api = api;
  }

  /** Routed through the mesh to the coturn instance co-located with the meeting's videoserver. */
  public Optional<TurnCredentials> getCredentials(String userId, String serviceId) {
    try {
      return toCredentials(api.internalTurnCredentialsGet(userId, Map.of(SERVICE_ID_HEADER, serviceId)));
    } catch (ApiException e) {
      throw new TurnServerException("Failed to get TURN credentials for user " + userId, e.getCode(), e);
    }
  }

  public Optional<TurnCredentials> getCredentialsAny(String userId) {
    try {
      return toCredentials(api.internalTurnCredentialsGet(userId));
    } catch (ApiException e) {
      throw new TurnServerException("Failed to get TURN credentials (fallback) for user " + userId, e.getCode(), e);
    }
  }

  private static Optional<TurnCredentials> toCredentials(TurnCredentialsDto dto) {
    if (!Boolean.TRUE.equals(dto.getConfigured())) {
      return Optional.empty();
    }
    return Optional.of(new TurnCredentials(
        dto.getUrl(), dto.getUsername(), dto.getCredential(), dto.getTtl() == null ? 0L : dto.getTtl()));
  }
}
