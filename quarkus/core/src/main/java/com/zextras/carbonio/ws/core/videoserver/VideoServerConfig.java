// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

@ConfigMapping(prefix = "ws.videoserver")
public interface VideoServerConfig {
  String url();

  String apiSecret();

  int bitrate();

  boolean bitrateCap();

  /** Consul service_id of the videoserver; absent on a single-videoserver (CE) installation. */
  Optional<String> serverId();
}
