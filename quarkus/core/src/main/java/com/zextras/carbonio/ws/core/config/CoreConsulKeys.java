// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.config;

import com.zextras.carbonio.ws.plugin.api.ConsulKeyMapping;
import java.util.Map;

/** Same Consul KV keys the Guice build reads (CoreConfigContribution). */
public class CoreConsulKeys implements ConsulKeyMapping {
  @Override
  public Map<String, String> consulKeys() {
    return Map.of(
        "ws.videoserver.api-secret", "carbonio-videoserver/api-secret",
        "ws.videoserver.bitrate", "carbonio-ws-collaboration/meeting/videoroom-bitrate",
        "ws.videoserver.bitrate-cap", "carbonio-ws-collaboration/meeting/videoroom-bitrate-cap",
        "ws.rabbitmq.username", "carbonio-message-broker/default/username",
        "ws.rabbitmq.password", "carbonio-message-broker/default/password",
        "quarkus.datasource.username", "carbonio-ws-collaboration-db/db-username",
        "quarkus.datasource.password", "carbonio-ws-collaboration-db/db-password");
  }
}
