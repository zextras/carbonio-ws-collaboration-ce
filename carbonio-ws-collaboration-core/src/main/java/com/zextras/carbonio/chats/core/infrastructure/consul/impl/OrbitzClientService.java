// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.consul.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import com.zextras.carbonio.chats.core.infrastructure.consul.ConsulService;
import java.util.List;
import java.util.UUID;

@Singleton
public class OrbitzClientService implements ConsulService {

  private final Consul consul;

  @Inject
  public OrbitzClientService(Consul consul) {
    this.consul = consul;
  }

  @Override
  public List<UUID> getHealthyServices(String serviceName, String idMetadata) {
    return consul.healthClient().getHealthyServiceInstances(serviceName).getResponse().stream()
        .map(ServiceHealth::getService)
        .map(Service::getMeta)
        .filter(service -> service.containsKey(idMetadata))
        .map(service -> UUID.fromString(service.get(idMetadata)))
        .toList();
  }
}
