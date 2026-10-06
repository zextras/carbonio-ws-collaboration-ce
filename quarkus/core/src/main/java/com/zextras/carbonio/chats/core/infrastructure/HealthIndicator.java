// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure;

public interface HealthIndicator {

  /**
   * Returns whether we can communicate with the component or not
   *
   * @return a {@link Boolean} which indicates if we can communicate with the component or not
   */
  boolean isAlive();
}
