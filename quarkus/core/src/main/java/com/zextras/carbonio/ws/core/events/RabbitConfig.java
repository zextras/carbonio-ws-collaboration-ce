// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.events;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "ws.rabbitmq")
public interface RabbitConfig {
  String host();

  int port();

  String username();

  String password();

  String virtualHost();
}
