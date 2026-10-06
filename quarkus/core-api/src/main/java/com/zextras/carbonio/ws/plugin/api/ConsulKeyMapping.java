// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import java.util.Map;

/**
 * Loaded with {@link java.util.ServiceLoader} (config is resolved before CDI starts). Register
 * implementations in {@code META-INF/services/com.zextras.carbonio.ws.plugin.api.ConsulKeyMapping}.
 */
public interface ConsulKeyMapping {
  /** Config property name → Consul KV key. */
  Map<String, String> consulKeys();
}
