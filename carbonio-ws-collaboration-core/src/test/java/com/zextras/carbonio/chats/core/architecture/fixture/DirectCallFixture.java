// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture.fixture;

import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import io.ebean.annotation.Transactional;

public class DirectCallFixture {

  private final EventDispatcher eventDispatcher;

  public DirectCallFixture(EventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Transactional
  public void violate() {
    eventDispatcher.sendToUserExchange("user", null);
  }
}
