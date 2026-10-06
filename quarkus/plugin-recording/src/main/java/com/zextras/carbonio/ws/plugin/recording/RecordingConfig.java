// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "ws.video-recorder")
public interface RecordingConfig {
  @WithDefault("127.78.0.4")
  String host();

  @WithDefault("20007")
  int port();

  @WithDefault("/var/lib/videoserver/recordings/")
  String recordingsPath();
}
