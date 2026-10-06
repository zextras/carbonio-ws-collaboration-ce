// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.zextras.carbonio.ws.plugin.api.ConsulKeyMapping;
import java.util.Map;

public class RecordingConsulKeys implements ConsulKeyMapping {
  @Override
  public Map<String, String> consulKeys() {
    return Map.of("ws.video-recorder.recordings-path", "carbonio-videorecorder/recordings-path");
  }
}
