// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.architecture.fixture;

import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import io.ebean.annotation.Transactional;
import java.util.List;

public class CleanFixture {

  private final EventDispatcher eventDispatcher;

  public CleanFixture(EventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Transactional
  public int dbOnly(List<String> ids) {
    return ids.stream().map(String::length).reduce(0, Integer::sum) + helper();
  }

  private int helper() {
    return 1;
  }

  public void afterCommit() {
    eventDispatcher.sendToUserExchange("user", null);
  }
}
