// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.provider;

import java.util.Optional;

public interface AppInfoProvider {

  /**
   * Retrieves the current Chats chats-information
   *
   * @return the current chats chats-information
   */
  Optional<String> getVersion();
}
