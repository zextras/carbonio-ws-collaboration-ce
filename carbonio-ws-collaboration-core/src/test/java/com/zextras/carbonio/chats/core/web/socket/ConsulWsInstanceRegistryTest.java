// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsulWsInstanceRegistryTest {

  private static final String SERVICE_NAME = "carbonio-ws-collaboration";

  private Consul consul;
  private AgentClient agentClient;
  private HealthClient healthClient;

  @BeforeEach
  void setUp() {
    consul = mock(Consul.class);
    agentClient = mock(AgentClient.class);
    healthClient = mock(HealthClient.class);
    when(consul.agentClient()).thenReturn(agentClient);
    when(consul.healthClient()).thenReturn(healthClient);
  }

  private static Service service(String name, Map<String, String> meta) {
    Service service = mock(Service.class);
    when(service.getService()).thenReturn(name);
    when(service.getMeta()).thenReturn(meta);
    return service;
  }

  private void agentServices(Service... services) {
    Map<String, Service> byId = new java.util.LinkedHashMap<>();
    for (int i = 0; i < services.length; i++) {
      byId.put("s" + i, services[i]);
    }
    when(agentClient.getServices()).thenReturn(byId);
  }

  @SuppressWarnings("unchecked")
  private void healthyInstances(Service... services) {
    ConsulResponse<List<ServiceHealth>> response = mock(ConsulResponse.class);
    List<ServiceHealth> instances =
        java.util.Arrays.stream(services)
            .map(
                svc -> {
                  ServiceHealth health = mock(ServiceHealth.class);
                  when(health.getService()).thenReturn(svc);
                  return health;
                })
            .toList();
    when(response.getResponse()).thenReturn(instances);
    when(healthClient.getHealthyServiceInstances(SERVICE_NAME)).thenReturn(response);
  }

  @Test
  @DisplayName("The own id is the service_id meta of the local agent service")
  void selfId_agentServiceWithMeta_returnsIt() {
    String id = UUID.randomUUID().toString();
    agentServices(
        service("other", Map.of("service_id", UUID.randomUUID().toString())),
        service(SERVICE_NAME, Map.of("service_id", id)));

    assertEquals(id, new ConsulWsInstanceRegistry(consul).selfId());
  }

  @Test
  @DisplayName("The own id is normalised like the live ids")
  void selfId_uppercaseMeta_isLowercaseCanonical() {
    String id = UUID.randomUUID().toString();
    agentServices(service(SERVICE_NAME, Map.of("service_id", id.toUpperCase())));
    healthyInstances(service(SERVICE_NAME, Map.of("service_id", id.toUpperCase())));

    ConsulWsInstanceRegistry registry = new ConsulWsInstanceRegistry(consul);

    assertEquals(id, registry.selfId());
    assertEquals(Set.of(id), registry.liveInstanceIds());
  }

  @Test
  @DisplayName("A missing service_id meta fails the construction")
  void constructor_noServiceId_throws() {
    agentServices(service(SERVICE_NAME, Map.of()));

    RuntimeException e =
        assertThrows(RuntimeException.class, () -> new ConsulWsInstanceRegistry(consul));
    assertEquals(
        "service_id not found in Consul service metadata for 'carbonio-ws-collaboration'."
            + " Ensure the service is registered with meta { service_id = \"...\" }",
        e.getMessage());
  }

  @Test
  @DisplayName("A service_id that is not a UUID fails the construction")
  void constructor_invalidUuid_throws() {
    agentServices(service(SERVICE_NAME, Map.of("service_id", "not-a-uuid")));

    assertThrows(IllegalArgumentException.class, () -> new ConsulWsInstanceRegistry(consul));
  }

  @Test
  @DisplayName("Live ids are the service_id metas of the healthy instances")
  void liveInstanceIds_healthyInstances_returnsTheirIds() {
    String own = UUID.randomUUID().toString();
    String peer = UUID.randomUUID().toString();
    agentServices(service(SERVICE_NAME, Map.of("service_id", own)));
    healthyInstances(
        service(SERVICE_NAME, Map.of("service_id", own)),
        service(SERVICE_NAME, Map.of("service_id", peer)),
        service(SERVICE_NAME, Map.of()));

    assertEquals(Set.of(own, peer), new ConsulWsInstanceRegistry(consul).liveInstanceIds());
  }

  @Test
  @DisplayName("A peer with a malformed service_id is skipped")
  void liveInstanceIds_malformedPeer_returnsOnlyValidIds() {
    String own = UUID.randomUUID().toString();
    agentServices(service(SERVICE_NAME, Map.of("service_id", own)));
    healthyInstances(
        service(SERVICE_NAME, Map.of("service_id", own)),
        service(SERVICE_NAME, Map.of("service_id", "not-a-uuid")));

    assertEquals(Set.of(own), new ConsulWsInstanceRegistry(consul).liveInstanceIds());
  }
}
