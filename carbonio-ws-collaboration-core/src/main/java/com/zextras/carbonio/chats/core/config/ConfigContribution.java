// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Extension seam for configuration keys. Each edition (CE, Advanced) contributes the config keys it
 * owns; the config implementations merge every bound contribution into a single catalog instead of
 * embedding a hardcoded union. Bound through a Guice {@code Multibinder<ConfigContribution>}, so
 * {@code Modules.override(CoreModule).with(AdvancedModule)} aggregates the sets across editions.
 */
public interface ConfigContribution {

  default Map<ConfigName, String> consulKvMappings() {
    return Collections.emptyMap();
  }

  default Set<ConfigName> environmentKeys() {
    return Collections.emptySet();
  }

  default Map<ConfigName, String> infrastructureDefaults() {
    return Collections.emptyMap();
  }
}
