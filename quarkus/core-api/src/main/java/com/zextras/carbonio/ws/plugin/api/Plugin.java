// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

/** Every plugin exposes exactly one CDI bean implementing this interface. */
public interface Plugin {
  PluginDescriptor descriptor();
}
