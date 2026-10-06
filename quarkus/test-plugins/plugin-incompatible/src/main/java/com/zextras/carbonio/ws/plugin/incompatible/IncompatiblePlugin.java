// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.incompatible;

import com.zextras.carbonio.ws.plugin.api.Plugin;
import com.zextras.carbonio.ws.plugin.api.PluginDescriptor;
import jakarta.enterprise.context.ApplicationScoped;

/** Built against a future plugin API: core 1.0 must refuse to start with it. */
@ApplicationScoped
public class IncompatiblePlugin implements Plugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("incompatible", "0.1.0", "2.0");
  }
}
