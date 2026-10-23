// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.core.infrastructure.consul.DiscoveryService;
import com.zextras.carbonio.chats.core.infrastructure.consul.impl.DockerDiscoveryService;

public class DockerDiscoveryModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DiscoveryService.class).to(DockerDiscoveryService.class);
  }
}
