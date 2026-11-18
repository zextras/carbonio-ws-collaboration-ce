// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture.fixture;

import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import io.ebean.annotation.Transactional;

public class PrivateHelperFixture {

  private final EventDispatcher eventDispatcher;

  public PrivateHelperFixture(EventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Transactional
  public void violate() {
    helper();
  }

  private void helper() {
    deeper();
  }

  private void deeper() {
    eventDispatcher.sendToUserExchange("user", null);
  }
}
