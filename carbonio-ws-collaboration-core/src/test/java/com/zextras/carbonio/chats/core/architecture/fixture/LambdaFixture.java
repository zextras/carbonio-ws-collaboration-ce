// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture.fixture;

import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import io.ebean.annotation.Transactional;
import java.util.List;

public class LambdaFixture {

  private final EventDispatcher eventDispatcher;

  public LambdaFixture(EventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Transactional
  public void violate(List<String> subscribers) {
    subscribers.forEach(user -> eventDispatcher.sendToUserExchange(user, null));
  }
}
