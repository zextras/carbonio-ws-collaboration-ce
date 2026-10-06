// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.zextras.carbonio.ws.plugin.api.Plugin;
import com.zextras.carbonio.ws.plugin.api.PluginDescriptor;
import com.zextras.carbonio.ws.plugin.api.PluginMigrations;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecordingPlugin implements Plugin, PluginMigrations {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("recording", "0.1.0", "1.0");
  }

  @Override
  public String pluginId() {
    return "recording";
  }

  @Override
  public String location() {
    return "db/migration/recording";
  }
}
