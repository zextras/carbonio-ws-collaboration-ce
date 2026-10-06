// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class TurnServerService {

  /** 503 upstream unavailable, 404 no matching instance, 0 network failure/timeout. */
  private static final Set<Integer> UNREACHABLE = Set.of(503, 404, 0);

  private final TurnServerClient client;

  @Inject
  public TurnServerService(TurnServerClient client) {
    this.client = client;
  }

  public Optional<TurnCredentials> getCredentials(String userId, String serviceId) {
    try {
      return client.getCredentials(userId, serviceId);
    } catch (TurnServerException e) {
      if (UNREACHABLE.contains(e.getCode())) {
        return client.getCredentialsAny(userId);
      }
      throw e;
    }
  }
}
