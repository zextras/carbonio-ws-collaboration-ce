// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.health.ServiceHealth;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Identifies this instance and its live peers through the Consul catalog, using {@code
 * Service.Meta["service_id"]} populated by the {@code meta { service_id = "..." }} block of the
 * service registration.
 */
@Singleton
public class ConsulWsInstanceRegistry implements WsInstanceRegistry {

  private static final String SERVICE_NAME = "carbonio-ws-collaboration";
  private static final String SERVICE_ID_METADATA = "service_id";

  private final Consul consul;
  private final String selfId;

  @Inject
  public ConsulWsInstanceRegistry(AppConfig appConfig) {
    this(buildClient(appConfig));
  }

  ConsulWsInstanceRegistry(Consul consul) {
    this.consul = consul;
    this.selfId = resolveSelfId(consul);
  }

  @Override
  public String selfId() {
    return selfId;
  }

  @Override
  public Set<String> liveInstanceIds() {
    Set<String> ids = new HashSet<>();
    boolean invalidFound = false;
    for (ServiceHealth health :
        consul.healthClient().getHealthyServiceInstances(SERVICE_NAME).getResponse()) {
      Map<String, String> meta = health.getService().getMeta();
      String id = meta == null ? null : meta.get(SERVICE_ID_METADATA);
      if (id == null || id.isBlank()) {
        continue;
      }
      try {
        ids.add(UUID.fromString(id).toString());
      } catch (IllegalArgumentException e) {
        invalidFound = true;
      }
    }
    if (invalidFound) {
      ChatsLogger.warn("Ignoring peer instances with an invalid service_id in Consul metadata");
    }
    return ids;
  }

  private static Consul buildClient(AppConfig appConfig) {
    try {
      Consul.Builder builder =
          Consul.builder()
              .withUrl(
                  new URL(
                      "http",
                      appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
                      appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
                      ""))
              .withPing(false);
      String token = System.getenv("CONSUL_HTTP_TOKEN");
      if (token != null && !token.isBlank()) {
        builder.withTokenAuth(token);
      }
      return builder.build();
    } catch (Exception e) {
      throw new ConsulException("Unable to connect to consul ", e);
    }
  }

  private static String resolveSelfId(Consul consul) {
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
    return UUID.fromString(value).toString();
  }
}
