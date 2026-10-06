// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import com.zextras.carbonio.ws.plugin.api.Plugin;
import com.zextras.carbonio.ws.plugin.api.PluginDescriptor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TurnPlugin implements Plugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("turn", "0.1.0", "1.0");
  }
}
