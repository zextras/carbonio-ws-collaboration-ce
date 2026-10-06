// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "ws.turn-server")
public interface TurnConfig {
  @WithDefault("127.78.0.4")
  String host();

  @WithDefault("20011")
  int port();
}
