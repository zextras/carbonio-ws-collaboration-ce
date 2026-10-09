// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface ConfigContribution {

  default Map<String, String> consulKvMappings() {
    return Collections.emptyMap();
  }

  default Set<String> environmentKeys() {
    return Collections.emptySet();
  }

  default Map<String, String> infrastructureDefaults() {
    return Collections.emptyMap();
  }
}
