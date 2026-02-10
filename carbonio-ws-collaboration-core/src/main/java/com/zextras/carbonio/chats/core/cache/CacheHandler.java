// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.usermanagement.entities.UserMyself;
import java.time.Duration;

@Singleton
public class CacheHandler {

  private final Cache<String, UserMyself> userMyselfCache;

  @Inject
  public CacheHandler() {
    this.userMyselfCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(60)).build();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(this.userMyselfCache::invalidateAll, "Cache handler shutdown hook"));
  }

  public Cache<String, UserMyself> getUserMyselfCache() {
    return userMyselfCache;
  }
}
