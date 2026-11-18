// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture.fixture;

import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import io.ebean.annotation.Transactional;

public class TransactionalCalleeFixture {

  private final EventDispatcher eventDispatcher;

  public TransactionalCalleeFixture(EventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Transactional
  public void outer() {
    inner();
  }

  @Transactional
  public void inner() {
    eventDispatcher.sendToUserExchange("user", null);
  }
}
