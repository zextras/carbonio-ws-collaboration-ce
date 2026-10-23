// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.consul.impl;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.infrastructure.consul.DiscoveryService;
import java.util.List;
import java.util.UUID;

@Singleton
public class DockerDiscoveryService implements DiscoveryService {

  @Override
  public List<UUID> getHealthyServices(String serviceName, String idMetadata) {
    return List.of(UUID.randomUUID());
  }

  @Override
  public boolean isServiceAvailable(String serviceName) {
    return true;
  }
}
