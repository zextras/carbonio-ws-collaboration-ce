// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.orbitz.consul.Consul;
import java.util.UUID;

/**
 * Resolves and caches the service identifier for the current instance.
 *
 * <p>The service identifier is a UUID read from the Consul service catalog metadata ({@code
 * Service.Meta["service_id"]}), matching the same pattern used by VideoServer. The metadata is
 * populated by the {@code meta { service_id = "..." }} block in the HCL service registration.
 */
@Singleton
public class ServiceIdProvider {

  private static final String SERVICE_NAME = "carbonio-ws-collaboration";
  private static final String SERVICE_ID_METADATA = "service_id";

  private final String serviceId;

  @Inject
  public ServiceIdProvider(Consul consul) {
    this.serviceId = resolveServiceId(consul);
  }

  /** Returns the service identifier for this instance. */
  public String get() {
    return serviceId;
  }

  private static String resolveServiceId(Consul consul) {
    String value =
        consul.agentClient().getServices().values().stream()
            .filter(svc -> SERVICE_NAME.equals(svc.getService()))
            .map(svc -> svc.getMeta().get(SERVICE_ID_METADATA))
            .filter(id -> id != null && !id.isBlank())
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "service_id not found in Consul service metadata for '"
                            + SERVICE_NAME
                            + "'. Ensure the service is registered with meta { service_id = \"...\""
                            + " }"));
    UUID.fromString(value);
    return value;
  }
}
