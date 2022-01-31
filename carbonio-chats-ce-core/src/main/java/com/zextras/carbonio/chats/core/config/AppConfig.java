// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import java.util.Optional;

public interface AppConfig {

  /**
   * Retrieves the specified config or returns null
   *
   * @param clazz the configuration parameter class (es. {@link Integer}, {@link Boolean} or {@link String})
   * @param key the configuration name
   * @param <T> the configuration parameter type
   * @return an {@link Optional} which contains the configuration, if found
   */
  <T> Optional<T> get(Class<T> clazz, String key);
}
