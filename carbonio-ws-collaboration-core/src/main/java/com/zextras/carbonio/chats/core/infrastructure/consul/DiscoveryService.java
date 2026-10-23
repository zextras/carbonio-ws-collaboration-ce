// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.consul;

import java.util.List;
import java.util.UUID;

public interface DiscoveryService {

  List<UUID> getHealthyServices(String serviceName, String idMetadata);

  boolean isServiceAvailable(String serviceName);
}
